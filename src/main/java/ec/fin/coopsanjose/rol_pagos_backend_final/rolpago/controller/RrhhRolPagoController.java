package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ListadoPeriodoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoCargaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoResumenDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.CargaRolesPagoService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.GestionRolesPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/rrhh/periodos/{periodo}/roles-pago")
@RequiredArgsConstructor
public class RrhhRolPagoController {

    private final CargaRolesPagoService cargaService;
    private final GestionRolesPagoService gestionService;

    /** Carga masiva: form-data con varios archivos "archivos". El período va en la URL (yyyy-MM). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResultadoCargaDto> cargar(@PathVariable String periodo,
                                                 @RequestParam("archivos") List<MultipartFile> archivos) {
        ResultadoCargaDto resultado = cargaService.cargar(parsear(periodo), archivos);
        return ApiResponse.ok(resultado.rechazados() == 0
                ? "Carga completada" : "Carga completada con archivos rechazados", resultado);
    }

    @GetMapping
    public ApiResponse<ListadoPeriodoDto> listar(@PathVariable String periodo) {
        return ApiResponse.ok("Roles de pago del período", gestionService.listar(parsear(periodo)));
    }

    @PostMapping("/{id}/inactivar")
    public ApiResponse<RolPagoResumenDto> inactivar(@PathVariable String periodo, @PathVariable Long id) {
        return ApiResponse.ok("Rol de pago inactivado", gestionService.inactivar(parsear(periodo), id));
    }

    @PostMapping("/{id}/activar")
    public ApiResponse<RolPagoResumenDto> activar(@PathVariable String periodo, @PathVariable Long id) {
        return ApiResponse.ok("Rol de pago activado", gestionService.activar(parsear(periodo), id));
    }

    private static YearMonth parsear(String periodo) {
        try {
            return YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            throw new SolicitudInvalidaException("Período inválido. Formato esperado: yyyy-MM (ej. 2026-08)");
        }
    }
}
