package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.AuditoriaAccionesService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.PeriodoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config.AdProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.NotificacionProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreo;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnvioCorreoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.PlantillaAviso;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ResultadoReenvioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.repository.NotificacionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.NotificacionRolDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/**
 * Reenvía a mano el aviso de UN rol ya publicado. El correo se arma con el código de usuario más el dominio institucional,
 * y el contenido lo define la plantilla de la API. El resultado queda en el aviso (estado, fecha y cantidad de reenvíos)
 * y en la bitácora.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReenvioNotificacionService {

    private static final String SIN_CUENTA = "No hay una cuenta activa en el AD para armar el correo de esta persona";
    private static final int LARGO_ERROR = 200;

    private final NotificacionRolPagoRepository repositorio;
    private final PeriodoRepository periodoRepository;
    private final RolPagoRepository rolPagoRepository;
    private final DirectorioAdRepository directorioRepository;
    private final NotificacionProperties propiedades;
    private final EnviadorCorreo enviador;
    private final AdProperties ad;
    private final AuditoriaAccionesService auditoria;
    private final Clock clock;

    public ResultadoReenvioDto reenviar(YearMonth ym, long rolPagoId) {
        if (!propiedades.habilitada()) {
            throw new ConflictoException("Los avisos por correo están deshabilitados en este entorno");
        }
        Periodo periodo = periodoRepository.findByFechaPeriodo(ym.atDay(1))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el período " + ym));
        if (periodo.getEstadoPeriodo() != EstadoPeriodo.PUBLICADO) {
            throw new ConflictoException("El período " + ym + " aún no está publicado: no hay avisos que reenviar");
        }
        RolPago rol = rolPagoRepository.findById(rolPagoId)
                .filter(r -> r.getPeriodo().getId().equals(periodo.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ese rol en el período " + ym));
        if (rol.getEstadoRegistro() != EstadoRegistro.ACTIVO) {
            throw new ConflictoException("El rol está inactivo: no tiene aviso que reenviar");
        }
        DirectorioAd cuenta = cuentaActiva(rol.getCedula());
        String correo = cuenta == null ? null : correoDe(cuenta.getUsernameAd());
        if (correo == null) {
            throw new ConflictoException(SIN_CUENTA);
        }
        NotificacionRolPago aviso = avisoDe(periodo, rol);
        if (aviso != null && aviso.getEstado() == EstadoNotificacion.PENDIENTE) {
            throw new ConflictoException("El aviso de esta persona está en cola de envío: espere a que salga");
        }
        EstadoNotificacion anterior = aviso == null ? null : aviso.getEstado();
        if (aviso == null) {
            aviso = nuevoAviso(periodo, rol);
        }
        aviso.setUsernameDestino(cuenta.getUsernameAd());
        aviso.setEmailDestino(correo);
        if (aviso.getId() == null) {
            aviso = repositorio.save(aviso);   // hace falta su número para las variables de la plantilla
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        String error = intentarReenvio(ym, aviso, correo);

        aviso.setReenviadaEn(ahora);
        aviso.setReenvios(aviso.getReenvios() + 1);
        aviso.setUltimoIntento(ahora);
        aviso.setProximoIntento(null);
        if (error == null) {
            aviso.setEstado(EstadoNotificacion.ENVIADA);
            aviso.setUltimoError(null);
            if (anterior != EstadoNotificacion.ENVIADA || aviso.getEnviadaEn() == null) {
                aviso.setEnviadaEn(ahora);
            }
        } else {
            aviso.setEstado(EstadoNotificacion.FALLIDA);
            aviso.setUltimoError(error.length() > LARGO_ERROR ? error.substring(0, LARGO_ERROR) : error);
        }
        aviso = repositorio.save(aviso);
        auditoria.registrar(TipoAccion.NOTIFICACION_REENVIADA, ym.toString(), rol.getId(), "aceptado=" + (error == null));
        log.info("Reenvío del aviso {} ({}): {}", aviso.getId(), ym, error == null ? "aceptado" : "no aceptado");
        return new ResultadoReenvioDto(error == null,
                error == null ? "La API de correo aceptó el reenvío" : error, NotificacionRolDto.de(aviso));
    }

    private String intentarReenvio(YearMonth ym, NotificacionRolPago aviso, String correo) {
        try {
            Map<String, String> variables = PlantillaAviso.variables(ym, propiedades.urlSistema(), correo,
                    aviso.getId(), LocalDate.now(clock));
            enviador.reenviar(correo, PlantillaAviso.asunto(ym), PlantillaAviso.cuerpo(ym, propiedades.urlSistema()),
                    "reenvio-" + aviso.getId() + "-" + (aviso.getReenvios() + 1), variables);
            return null;
        } catch (EnvioCorreoException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            return "Error inesperado al reenviar (" + e.getClass().getSimpleName() + ")";
        }
    }

    private NotificacionRolPago avisoDe(Periodo periodo, RolPago rol) {
        return repositorio.findByPeriodo_Id(periodo.getId()).stream()
                .filter(a -> rol.getCedula().equals(a.getCedula())).findFirst().orElse(null);
    }

    private static NotificacionRolPago nuevoAviso(Periodo periodo, RolPago rol) {
        NotificacionRolPago aviso = new NotificacionRolPago();
        aviso.setPeriodo(periodo);
        aviso.setRolPagoId(rol.getId());
        aviso.setCedula(rol.getCedula());
        aviso.setEstado(EstadoNotificacion.OMITIDA);   // sin próximo intento: el despachador no lo toma
        return aviso;
    }

    /** La cuenta activa de creación más reciente, igual que al encolar los avisos. */
    private DirectorioAd cuentaActiva(String cedula) {
        return directorioRepository.findByCedula(cedula).stream()
                .filter(d -> d.getEstadoRegistro() == EstadoRegistro.ACTIVO)
                .max(Comparator.comparing(DirectorioAd::getFechaCreacionAd, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(DirectorioAd::getUsernameAd))
                .orElse(null);
    }

    /** Código de usuario + @ + dominio institucional (por ejemplo fsepa@coopsanjose.fin.ec). */
    private String correoDe(String username) {
        String dominio = ad.dominioCorreo();
        if (username == null || username.isBlank() || dominio == null || dominio.isBlank()) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT) + "@" + dominio.trim();
    }
}
