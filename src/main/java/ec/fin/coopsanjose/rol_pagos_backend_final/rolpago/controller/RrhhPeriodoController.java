package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PeriodoResumenDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.ListadoPeriodosService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rrhh/periodos")
@RequiredArgsConstructor
public class RrhhPeriodoController {

    private final ListadoPeriodosService servicio;

    @GetMapping
    public ApiResponse<List<PeriodoResumenDto>> listar() {
        return ApiResponse.ok("Períodos", servicio.listar());
    }
}
