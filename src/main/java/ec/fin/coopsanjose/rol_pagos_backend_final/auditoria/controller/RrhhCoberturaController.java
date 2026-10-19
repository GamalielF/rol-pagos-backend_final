package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.CoberturaPeriodoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.ReportesAuditoriaService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/rrhh/periodos/{periodo}")
@RequiredArgsConstructor
public class RrhhCoberturaController {

    private final ReportesAuditoriaService servicio;

    /** ¿Quién ya abrió su rol y quién no? Solo para períodos publicados. */
    @GetMapping("/cobertura")
    public ApiResponse<CoberturaPeriodoDto> cobertura(@PathVariable String periodo) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            throw new SolicitudInvalidaException("Período inválido. Formato esperado: yyyy-MM (ej. 2026-08)");
        }
        return ApiResponse.ok("Cobertura de lectura del período", servicio.cobertura(ym));
    }
}
