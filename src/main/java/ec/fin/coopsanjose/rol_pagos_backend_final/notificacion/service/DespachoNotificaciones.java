package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.NotificacionProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreo;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreoConVariables;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnvioCorreoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.PlantillaAviso;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.repository.NotificacionRolPagoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.Map;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Envía un lote de avisos pendientes. Cada aviso se guarda en su propia transacción: si el proceso se corta
 * en la mitad, lo ya enviado queda registrado. Pensado para UNA instancia de la aplicación.
 */
@Slf4j
@Service
public class DespachoNotificaciones {

    /** Entrega un aviso: si el enviador acepta variables (la API de correo) se las pasa; si no, usa la forma simple. */
    static void entregar(EnviadorCorreo enviador, String destinatario, String asunto, String cuerpo,
                         String referencia, Map<String, String> variables) {
        if (enviador instanceof EnviadorCorreoConVariables conVariables) {
            conVariables.enviar(destinatario, asunto, cuerpo, referencia, variables);
        } else {
            enviador.enviar(destinatario, asunto, cuerpo, referencia);
        }
    }

    public record ResultadoLote(int enviadas, int reintentos, int fallidas) {
        public int total() {
            return enviadas + reintentos + fallidas;
        }
    }

    private static final ResultadoLote NADA = new ResultadoLote(0, 0, 0);

    private final NotificacionRolPagoRepository repositorio;
    private final EnviadorCorreo enviador;
    private final NotificacionProperties propiedades;
    private final Clock clock;
    private final TransactionTemplate transaccion;
    private final ReentrantLock candado = new ReentrantLock();
    private final AtomicBoolean avisadoSinConfigurar = new AtomicBoolean(false);

    public DespachoNotificaciones(NotificacionRolPagoRepository repositorio, EnviadorCorreo enviador,
                                  NotificacionProperties propiedades, Clock clock,
                                  PlatformTransactionManager transactionManager) {
        this.repositorio = repositorio;
        this.enviador = enviador;
        this.propiedades = propiedades;
        this.clock = clock;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    public ResultadoLote despacharLote() {
        if (!propiedades.habilitada()) {
            return NADA;
        }
        if (!enviador.estaConfigurado()) {
            if (avisadoSinConfigurar.compareAndSet(false, true)) {
                log.warn("Hay avisos habilitados pero el envío de correo no está configurado (app.correo.api.url): los avisos esperan");
            }
            return NADA;
        }
        avisadoSinConfigurar.set(false);
        if (!candado.tryLock()) {
            return NADA;
        }
        try {
            return procesar();
        } finally {
            candado.unlock();
        }
    }

    private ResultadoLote procesar() {
        List<NotificacionRolPago> lote = repositorio.listasParaEnviar(EstadoNotificacion.PENDIENTE,
                LocalDateTime.now(clock), PageRequest.of(0, Math.max(1, propiedades.lote())));
        int enviadas = 0;
        int reintentos = 0;
        int fallidas = 0;
        for (NotificacionRolPago aviso : lote) {
            YearMonth periodo = YearMonth.from(aviso.getPeriodo().getFechaPeriodo());
            try {
                entregar(enviador, aviso.getEmailDestino(), PlantillaAviso.asunto(periodo),
                        PlantillaAviso.cuerpo(periodo, propiedades.urlSistema()), "aviso-" + aviso.getId(),
                        PlantillaAviso.variables(periodo, propiedades.urlSistema(), aviso.getEmailDestino(),
                                aviso.getId(), LocalDate.now(clock)));
                guardar(aviso, null);
                enviadas++;
            } catch (EnvioCorreoException e) {
                boolean definitivo = guardar(aviso, e);
                if (definitivo) {
                    fallidas++;
                } else {
                    reintentos++;
                }
                if (e.esReintentable()) {
                    // El servicio está caído o rechaza las credenciales: insistir con el resto solo lo castiga
                    log.warn("Se detiene el lote: {}", e.getMessage());
                    break;
                }
            } catch (RuntimeException e) {
                // Por ejemplo, otro usuario tocó el mismo aviso: se reintenta en la próxima vuelta
                log.warn("No se pudo registrar el resultado de un aviso ({})", e.getClass().getSimpleName());
            }
        }
        return new ResultadoLote(enviadas, reintentos, fallidas);
    }

    /** Anota el resultado. @return true si el aviso quedó FALLIDO de forma definitiva. */
    private boolean guardar(NotificacionRolPago aviso, EnvioCorreoException error) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        aviso.setIntentos(aviso.getIntentos() + 1);
        aviso.setUltimoIntento(ahora);
        boolean definitivo = false;
        if (error == null) {
            aviso.setEstado(EstadoNotificacion.ENVIADA);
            aviso.setEnviadaEn(ahora);
            aviso.setProximoIntento(null);
            aviso.setUltimoError(null);
        } else {
            aviso.setUltimoError(error.getMessage().length() > 200 ? error.getMessage().substring(0, 200) : error.getMessage());
            definitivo = !error.esReintentable() || aviso.getIntentos() >= propiedades.maxIntentos();
            if (definitivo) {
                aviso.setEstado(EstadoNotificacion.FALLIDA);
                aviso.setProximoIntento(null);
            } else {
                aviso.setEstado(EstadoNotificacion.PENDIENTE);
                aviso.setProximoIntento(ahora.plusMinutes(Math.min(60L, 1L << (aviso.getIntentos() - 1))));   // 1, 2, 4, 8... min
            }
        }
        transaccion.executeWithoutResult(estado -> repositorio.save(aviso));
        return definitivo;
    }
}
