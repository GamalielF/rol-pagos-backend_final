package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

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
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ListadoPeriodoDto;
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

/** Lo que RRHH hace con los roles ya cargados: verlos en lista y retirarlos o reponerlos antes de publicar. */
@Service
@RequiredArgsConstructor
public class GestionRolesPagoService {

    private final PeriodoRepository periodoRepository;
    private final RolPagoRepository rolPagoRepository;
    private final DirectorioAdRepository directorioRepository;
    private final AuditoriaAccionesService auditoria;

    @Transactional(readOnly = true)
    public ListadoPeriodoDto listar(YearMonth ym) {
        Periodo periodo = periodoRepository.findByFechaPeriodo(ym.atDay(1))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el período " + ym));
        List<RolPago> roles = rolPagoRepository.findByPeriodo_IdOrderByCedulaAsc(periodo.getId());
        Map<String, String> nombres = nombresPorCedula(roles.stream().map(RolPago::getCedula).toList());

        long activos = roles.stream().filter(r -> r.getEstadoRegistro() == EstadoRegistro.ACTIVO).count();
        return new ListadoPeriodoDto(ym.toString(), periodo.getEstadoPeriodo().name(), activos,
                roles.size() - activos, roles.stream().map(r -> aResumen(r, nombres)).toList());
    }

    @Transactional
    public RolPagoResumenDto inactivar(YearMonth ym, Long id) {
        RolPago rol = obtenerEditable(ym, id);
        rol.inactivar();
        RolPago guardado = rolPagoRepository.save(rol);
        auditoria.registrar(TipoAccion.ROL_INACTIVADO, ym.toString(), guardado.getId(), "Retirado por RRHH antes de publicar");
        return resumen(guardado);
    }

    @Transactional
    public RolPagoResumenDto activar(YearMonth ym, Long id) {
        RolPago rol = obtenerEditable(ym, id);
        rol.activar();
        RolPago guardado = rolPagoRepository.save(rol);
        auditoria.registrar(TipoAccion.ROL_ACTIVADO, ym.toString(), guardado.getId(), "Repuesto por RRHH antes de publicar");
        return resumen(guardado);
    }

    /** El rol debe pertenecer al período de la URL (evita tocar un rol ajeno cambiando el id) y no estar publicado. */
    private RolPago obtenerEditable(YearMonth ym, Long id) {
        RolPago rol = rolPagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El rol de pago no existe"));
        Periodo periodo = rol.getPeriodo();
        if (!periodo.getFechaPeriodo().equals(ym.atDay(1))) {
            throw new RecursoNoEncontradoException("El rol de pago no pertenece a ese período");
        }
        if (periodo.getEstadoPeriodo() == EstadoPeriodo.PUBLICADO) {
            throw new ConflictoException("El período ya fue publicado: sus roles no se modifican");
        }
        return rol;
    }

    private RolPagoResumenDto resumen(RolPago rol) {
        return aResumen(rol, nombresPorCedula(List.of(rol.getCedula())));
    }

    private RolPagoResumenDto aResumen(RolPago r, Map<String, String> nombres) {
        return new RolPagoResumenDto(r.getId(), enmascarar(r.getCedula()), nombres.get(r.getCedula()),
                r.getEstadoRegistro().name(), r.getTamanoBytes(), r.getModificadoEn(), r.getModificadoPor());
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

    private static String enmascarar(String cedula) {
        return (cedula == null || cedula.length() != 10) ? null : cedula.substring(0, 2) + "******" + cedula.substring(8);
    }
}
