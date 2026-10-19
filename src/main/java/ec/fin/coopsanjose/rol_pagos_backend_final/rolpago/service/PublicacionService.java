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
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PublicacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResumenPeriodoDto;
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
import java.util.Set;
import java.util.stream.Collectors;

/** Resumen previo y publicación de un período. Un período publicado ya no se modifica. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicacionService {

    private final PeriodoRepository periodoRepository;
    private final RolPagoRepository rolPagoRepository;
    private final DirectorioAdRepository directorioRepository;
    private final AuditoriaAccionesService auditoria;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ResumenPeriodoDto resumen(YearMonth ym) {
        return calcular(buscar(ym));
    }

    @Transactional
    public PublicacionDto publicar(YearMonth ym, boolean confirmarFaltantes, String publicadoPor) {
        Periodo periodo = buscar(ym);
        if (periodo.getEstadoPeriodo() == EstadoPeriodo.PUBLICADO) {
            throw new ConflictoException("El período " + ym + " ya fue publicado");
        }
        ResumenPeriodoDto resumen = calcular(periodo);
        if (resumen.rolesActivos() == 0) {
            throw new ConflictoException("El período no tiene roles activos para publicar");
        }
        if (resumen.sinRol() > 0 && !confirmarFaltantes) {
            throw new ConflictoException("Hay " + resumen.sinRol() + " personas activas sin rol de pago. "
                    + "Revise el resumen y confirme la publicación para continuar");
        }

        periodo.setEstadoPeriodo(EstadoPeriodo.PUBLICADO);
        periodo.setFechaPublicacion(LocalDateTime.now(clock));
        periodo.setPublicadoPor(publicadoPor);
        periodoRepository.save(periodo);
        auditoria.registrar(TipoAccion.PERIODO_PUBLICADO, ym.toString(), null,
                "roles=" + resumen.rolesActivos() + ", sinRol=" + resumen.sinRol()
                        + (confirmarFaltantes ? ", faltantes confirmados" : ""));


        log.info("Período {} publicado por {}: {} roles, {} personas sin rol",
                ym, publicadoPor, resumen.rolesActivos(), resumen.sinRol());
        return new PublicacionDto(ym.toString(), EstadoPeriodo.PUBLICADO.name(), periodo.getFechaPublicacion(),
                publicadoPor, resumen.rolesActivos(), resumen.sinRol());
    }

    private Periodo buscar(YearMonth ym) {
        return periodoRepository.findByFechaPeriodo(ym.atDay(1))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el período " + ym));
    }

    private ResumenPeriodoDto calcular(Periodo periodo) {
        List<RolPago> roles = rolPagoRepository.findByPeriodo_IdOrderByCedulaAsc(periodo.getId());
        long activos = roles.stream().filter(r -> r.getEstadoRegistro() == EstadoRegistro.ACTIVO).count();
        Set<String> conRolActivo = roles.stream()
                .filter(r -> r.getEstadoRegistro() == EstadoRegistro.ACTIVO)
                .map(RolPago::getCedula)
                .collect(Collectors.toSet());

        // Una persona = una cédula: varias cuentas de la misma persona cuentan una sola vez
        Map<String, String> personasActivas = new HashMap<>();
        directorioRepository.findAll().stream()
                .filter(d -> d.getEstadoRegistro() == EstadoRegistro.ACTIVO)
                .sorted(Comparator.comparing(DirectorioAd::getUsernameAd))
                .forEach(d -> personasActivas.putIfAbsent(d.getCedula(),
                        d.getNombreCompleto() == null ? "(sin nombre)" : d.getNombreCompleto()));

        List<String> sinRol = personasActivas.entrySet().stream()
                .filter(e -> !conRolActivo.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new ResumenPeriodoDto(YearMonth.from(periodo.getFechaPeriodo()).toString(),
                periodo.getEstadoPeriodo().name(), activos, roles.size() - activos,
                personasActivas.size(), sinRol.size(), sinRol);
    }
}
