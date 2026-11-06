package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.AuditoriaAccionesService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.PeriodoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.repository.NotificacionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ListadoPeriodoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.NotificacionRolDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoResumenDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lo que RRHH hace con los roles ya cargados: verlos en lista y retirarlos o reponerlos antes de publicar. */
@Service
@RequiredArgsConstructor
public class GestionRolesPagoService {

    private final PeriodoRepository periodoRepository;
    private final RolPagoRepository rolPagoRepository;
    private final DirectorioAdRepository directorioRepository;
    private final NotificacionRolPagoRepository notificaciones;
    private final AuditoriaAccionesService auditoria;

    @Transactional(readOnly = true)
    public ListadoPeriodoDto listar(YearMonth ym) {
        Periodo periodo = periodoRepository.findByFechaPeriodo(ym.atDay(1))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el período " + ym));
        List<RolPago> roles = rolPagoRepository.findByPeriodo_IdOrderByCedulaAsc(periodo.getId());
        Map<String, String> nombres = nombresPorCedula(roles.stream().map(RolPago::getCedula).toList());

        Map<String, NotificacionRolPago> avisos = notificaciones.findByPeriodo_Id(periodo.getId()).stream()
                .collect(Collectors.toMap(NotificacionRolPago::getCedula, Function.identity(), (primero, segundo) -> primero));

        long activos = roles.stream().filter(r -> r.getEstadoRegistro() == EstadoRegistro.ACTIVO).count();
        return new ListadoPeriodoDto(ym.toString(), periodo.getEstadoPeriodo().name(), activos,
                roles.size() - activos, roles.stream().map(r -> aResumen(r, nombres, avisos.get(r.getCedula()))).toList());
    }

    @Transactional
    public RolPagoResumenDto inactivar(YearMonth ym, Long id) {
        return inactivar(ym, id, null);
    }

    /** Antes de publicar, sin más trámite. Con el período ya publicado es una corrección: exige el motivo y lo deja en la auditoría. */
    @Transactional
    public RolPagoResumenDto inactivar(YearMonth ym, Long id, String motivo) {
        RolPago rol = obtenerDelPeriodo(ym, id);
        String motivoLimpio = motivoSiEstaPublicado(rol, motivo);
        rol.inactivar();
        RolPago guardado = rolPagoRepository.save(rol);
        auditoria.registrar(TipoAccion.ROL_INACTIVADO, ym.toString(), guardado.getId(),
                motivoLimpio == null ? "Retirado por RRHH antes de publicar"
                        : "Retirado por RRHH después de publicar; motivo=" + motivoLimpio);
        return resumen(guardado);
    }

    @Transactional
    public RolPagoResumenDto activar(YearMonth ym, Long id) {
        return activar(ym, id, null);
    }

    @Transactional
    public RolPagoResumenDto activar(YearMonth ym, Long id, String motivo) {
        RolPago rol = obtenerDelPeriodo(ym, id);
        String motivoLimpio = motivoSiEstaPublicado(rol, motivo);
        rol.activar();
        RolPago guardado = rolPagoRepository.save(rol);
        auditoria.registrar(TipoAccion.ROL_ACTIVADO, ym.toString(), guardado.getId(),
                motivoLimpio == null ? "Repuesto por RRHH antes de publicar"
                        : "Repuesto por RRHH después de publicar; motivo=" + motivoLimpio);
        return resumen(guardado);
    }

    /** El rol debe pertenecer al período de la URL (evita tocar un rol ajeno cambiando el id). */
    private RolPago obtenerDelPeriodo(YearMonth ym, Long id) {
        RolPago rol = rolPagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El rol de pago no existe"));
        if (!rol.getPeriodo().getFechaPeriodo().equals(ym.atDay(1))) {
            throw new RecursoNoEncontradoException("El rol de pago no pertenece a ese período");
        }
        return rol;
    }

    /** Con el período publicado cambiar un rol es una corrección: pide y valida el motivo. Antes de publicar devuelve null. */
    private static String motivoSiEstaPublicado(RolPago rol, String motivo) {
        return rol.getPeriodo().getEstadoPeriodo() == EstadoPeriodo.PUBLICADO ? MotivoCorreccion.validar(motivo) : null;
    }

    private RolPagoResumenDto resumen(RolPago rol) {
        return aResumen(rol, nombresPorCedula(List.of(rol.getCedula())), null);
    }

    private RolPagoResumenDto aResumen(RolPago r, Map<String, String> nombres, NotificacionRolPago aviso) {
        return new RolPagoResumenDto(r.getId(), r.getCedula(), nombres.get(r.getCedula()),
                r.getEstadoRegistro().name(), r.getTamanoBytes(), r.getModificadoEn(), r.getModificadoPor(),
                notificacionDe(aviso));
    }

    private static NotificacionRolDto notificacionDe(NotificacionRolPago aviso) {
        return NotificacionRolDto.de(aviso);
    }

    /** Un nombre por cédula: el de un registro ACTIVO si lo hay (las cédulas con varios códigos comparten persona). */
    private Map<String, String> nombresPorCedula(Collection<String> cedulas) {
        Map<String, String> nombres = new HashMap<>();
        if (cedulas.isEmpty()) {
            return nombres;
        }
        directorioRepository.findByCedulaIn(cedulas).stream()
                .sorted(Comparator.comparing((DirectorioAd d) -> d.getEstadoRegistro() != EstadoRegistro.ACTIVO)
                        .thenComparing(DirectorioAd::getUsernameAd))
                .forEach(d -> nombres.putIfAbsent(d.getCedula(), d.getNombreCompleto()));
        return nombres;
    }
}
