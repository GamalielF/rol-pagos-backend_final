package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.AuditoriaAccionesService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.PeriodoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service.NombresDirectorio;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config.AdProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.NotificacionProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreo;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.InformeNotificacionesDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ProblemaNotificacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ResultadoEncoladoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.repository.NotificacionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** La cola de avisos: encolar al publicar, informar a RRHH y reintentar. El envío real lo hace DespachoNotificaciones. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private static final String SIN_CUENTA = "No hay una cuenta activa con correo en el AD";

    /** Entre varias cuentas activas de una persona se avisa a la de creación más reciente (el último gana). */
    private static final Comparator<DirectorioAd> ORDEN_DESTINO =
            Comparator.comparing(DirectorioAd::getFechaCreacionAd, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(DirectorioAd::getUsernameAd);

    private final NotificacionRolPagoRepository repositorio;
    private final PeriodoRepository periodoRepository;
    private final RolPagoRepository rolPagoRepository;
    private final DirectorioAdRepository directorioRepository;
    private final NombresDirectorio nombresDirectorio;
    private final NotificacionProperties propiedades;
    private final EnviadorCorreo enviador;
    private final CorreoProperties correo;
    private final AdProperties ad;
    private final AuditoriaAccionesService auditoria;
    private final Clock clock;

    /** La llama la publicación, dentro de su transacción. Con los avisos apagados no hace nada. */
    @Transactional
    public ResultadoEncoladoDto encolarAlPublicar(Periodo periodo) {
        if (!propiedades.habilitada()) {
            log.info("Avisos por correo deshabilitados: no se encola nada");
            return new ResultadoEncoladoDto(0, 0, 0, 0);
        }
        ResultadoEncoladoDto resultado = encolar(periodo);
        log.info("Avisos encolados: {} pendientes, {} omitidos", resultado.pendientes(), resultado.omitidas());
        return resultado;
    }

    /** Idempotente: encola solo lo que falta, y da otra oportunidad a quien antes no tenía cuenta con correo. */
    @Transactional
    public ResultadoEncoladoDto generar(YearMonth ym) {
        exigirHabilitadas();
        ResultadoEncoladoDto resultado = encolar(buscarPublicado(ym));
        auditoria.registrar(TipoAccion.NOTIFICACIONES_GENERADAS, ym.toString(), null,
                "creadas=" + resultado.creadas() + ", pendientes=" + resultado.pendientes()
                        + ", omitidas=" + resultado.omitidas() + ", yaExistentes=" + resultado.yaExistentes());
        return resultado;
    }

    /** Devuelve los avisos FALLIDOS a la cola con los intentos en cero. */
    @Transactional
    public long reintentar(YearMonth ym) {
        exigirHabilitadas();
        Periodo periodo = buscarPublicado(ym);
        LocalDateTime ahora = LocalDateTime.now(clock);
        long reintentadas = 0;
        for (NotificacionRolPago n : repositorio.findByPeriodo_Id(periodo.getId())) {
            if (n.getEstado() == EstadoNotificacion.FALLIDA) {
                n.setEstado(EstadoNotificacion.PENDIENTE);
                n.setIntentos(0);
                n.setProximoIntento(ahora);
                n.setUltimoError(null);
                repositorio.save(n);
                reintentadas++;
            }
        }
        auditoria.registrar(TipoAccion.NOTIFICACIONES_REINTENTADAS, ym.toString(), null, "avisos=" + reintentadas);
        return reintentadas;
    }

    /**
     * Corrección de un período YA publicado: el aviso de ese rol vuelve a la cola aunque ya se hubiera enviado, para que la
     * persona sepa que su rol cambió. Si el rol es nuevo, crea su aviso. Con los avisos apagados no hace nada.
     * Corre dentro de la transacción de quien corrige: si algo falla, el rol y su aviso se deshacen juntos.
     */
    @Transactional
    public void reencolarPorCorreccion(Periodo periodo, RolPago rol, List<DirectorioAd> cuentasActivas) {
        if (!propiedades.habilitada()) {
            return;
        }
        NotificacionRolPago aviso = repositorio.findByPeriodo_IdAndCedula(periodo.getId(), rol.getCedula())
                .orElseGet(() -> {
                    NotificacionRolPago nuevo = new NotificacionRolPago();
                    nuevo.setPeriodo(periodo);
                    nuevo.setCedula(rol.getCedula());
                    return nuevo;
                });
        if (aviso.getEstado() == EstadoNotificacion.ENVIADA) {
            aviso.setReenvios(aviso.getReenvios() + 1);   // esta persona recibirá un segundo correo
        }
        aviso.setRolPagoId(rol.getId());
        DirectorioAd destino = cuentasActivas.stream().max(ORDEN_DESTINO).orElse(null);
        String correoDestino = destino == null ? null : ad.correoDe(destino.getUsernameAd());
        if (correoDestino == null) {
            aviso.setEstado(EstadoNotificacion.OMITIDA);
            aviso.setUltimoError(SIN_CUENTA);
        } else {
            aviso.setUsernameDestino(destino.getUsernameAd());
            aviso.setEmailDestino(correoDestino);
            aviso.setEstado(EstadoNotificacion.PENDIENTE);
            aviso.setIntentos(0);
            aviso.setProximoIntento(LocalDateTime.now(clock));
            aviso.setUltimoError(null);
        }
        repositorio.save(aviso);
    }

    @Transactional(readOnly = true)
    public InformeNotificacionesDto informe(YearMonth ym) {
        Periodo periodo = buscar(ym);
        List<NotificacionRolPago> todas = repositorio.findByPeriodo_Id(periodo.getId());

        long reintentando = todas.stream()
                .filter(n -> n.getEstado() == EstadoNotificacion.PENDIENTE && n.getIntentos() > 0).count();
        List<NotificacionRolPago> conProblema = todas.stream()
                .filter(n -> n.getEstado() == EstadoNotificacion.FALLIDA || n.getEstado() == EstadoNotificacion.OMITIDA)
                .toList();
        Map<String, String> nombres = nombresDirectorio.porCedula(
                conProblema.stream().map(NotificacionRolPago::getCedula).toList());
        List<ProblemaNotificacionDto> problemas = conProblema.stream()
                .map(n -> new ProblemaNotificacionDto(nombres.getOrDefault(n.getCedula(), "(sin nombre en el directorio)"),
                        n.getEstado().name(), n.getIntentos(), n.getUltimoError()))
                .sorted(Comparator.comparing(ProblemaNotificacionDto::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new InformeNotificacionesDto(ym.toString(), propiedades.habilitada(), todas.size(),
                contar(todas, EstadoNotificacion.PENDIENTE), reintentando, contar(todas, EstadoNotificacion.ENVIADA),
                contar(todas, EstadoNotificacion.FALLIDA), contar(todas, EstadoNotificacion.OMITIDA), problemas,
                propiedades.despachoAutomatico(), enviador.estaConfigurado(), correo.modo().name());
    }

    // ------------------------------------------------------------ interno

    private ResultadoEncoladoDto encolar(Periodo periodo) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        Map<String, NotificacionRolPago> existentes = repositorio.findByPeriodo_Id(periodo.getId()).stream()
                .collect(Collectors.toMap(NotificacionRolPago::getCedula, Function.identity()));
        Map<String, DirectorioAd> destinos = destinosPorCedula();

        long creadas = 0;
        long pendientes = 0;
        long omitidas = 0;
        long yaExistentes = 0;
        for (RolPago rol : rolPagoRepository.findByPeriodo_IdOrderByCedulaAsc(periodo.getId())) {
            if (rol.getEstadoRegistro() != EstadoRegistro.ACTIVO) {
                continue;
            }
            NotificacionRolPago aviso = existentes.get(rol.getCedula());
            if (aviso != null && aviso.getEstado() != EstadoNotificacion.OMITIDA) {
                yaExistentes++;
                continue;
            }
            if (aviso == null) {
                aviso = new NotificacionRolPago();
                aviso.setPeriodo(periodo);
                aviso.setRolPagoId(rol.getId());
                aviso.setCedula(rol.getCedula());
            }
            DirectorioAd destino = destinos.get(rol.getCedula());
            String correoDestino = destino == null ? null : ad.correoDe(destino.getUsernameAd());
            if (correoDestino == null) {
                aviso.setEstado(EstadoNotificacion.OMITIDA);
                aviso.setUltimoError(SIN_CUENTA);
                omitidas++;
            } else {
                aviso.setUsernameDestino(destino.getUsernameAd());
                aviso.setEmailDestino(correoDestino);
                aviso.setEstado(EstadoNotificacion.PENDIENTE);
                aviso.setProximoIntento(ahora);
                aviso.setUltimoError(null);
                pendientes++;
            }
            repositorio.save(aviso);
            creadas++;
        }
        return new ResultadoEncoladoDto(creadas, pendientes, omitidas, yaExistentes);
    }

    /** Una cuenta por persona: la ACTIVA de creación más reciente (el último gana). */
    private Map<String, DirectorioAd> destinosPorCedula() {
        Map<String, DirectorioAd> destinos = new HashMap<>();
        directorioRepository.findAllByEstadoRegistro(EstadoRegistro.ACTIVO).stream()
                .sorted(ORDEN_DESTINO)
                .forEach(d -> destinos.put(d.getCedula(), d));
        return destinos;
    }

    private void exigirHabilitadas() {
        if (!propiedades.habilitada()) {
            throw new ConflictoException("Los avisos por correo están deshabilitados en este entorno");
        }
    }

    private Periodo buscar(YearMonth ym) {
        return periodoRepository.findByFechaPeriodo(ym.atDay(1))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el período " + ym));
    }

    private Periodo buscarPublicado(YearMonth ym) {
        Periodo periodo = buscar(ym);
        if (periodo.getEstadoPeriodo() != EstadoPeriodo.PUBLICADO) {
            throw new ConflictoException("El período " + ym + " aún no está publicado: no hay avisos que enviar");
        }
        return periodo;
    }

    private static long contar(List<NotificacionRolPago> avisos, EstadoNotificacion estado) {
        return avisos.stream().filter(n -> n.getEstado() == estado).count();
    }
}