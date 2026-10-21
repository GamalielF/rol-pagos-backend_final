package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.PeriodoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PeriodoResumenDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListadoPeriodosService {

    private final PeriodoRepository periodoRepository;
    private final RolPagoRepository rolPagoRepository;

    /** Todos los períodos, del más reciente al más antiguo, con su estado y cuántos roles activos tienen. */
    @Transactional(readOnly = true)
    public List<PeriodoResumenDto> listar() {
        Map<Long, Long> conteos = rolPagoRepository.contarPorPeriodo(EstadoRegistro.ACTIVO).stream()
                .collect(Collectors.toMap(RolPagoRepository.ConteoPorPeriodo::getPeriodoId,
                        RolPagoRepository.ConteoPorPeriodo::getTotal));
        return periodoRepository.findAll().stream()
                .sorted(Comparator.comparing(Periodo::getFechaPeriodo).reversed())
                .map(p -> new PeriodoResumenDto(YearMonth.from(p.getFechaPeriodo()).toString(),
                        p.getEstadoPeriodo().name(), p.getFechaPublicacion(), p.getPublicadoPor(),
                        conteos.getOrDefault(p.getId(), 0L)))
                .toList();
    }
}
