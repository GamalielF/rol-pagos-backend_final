package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.request.FiltroAcciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.request.FiltroAccesos;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.AccesoAuditoriaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.AccionAuditoriaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.ArchivoDescargaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.DenegadosPorUsuarioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.ReportesAuditoriaService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.PaginaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Reportes de auditoría. Todas las fechas son yyyy-MM-dd; sin fechas, los últimos 30 días. */
@RestController
@RequestMapping("/admin/auditoria")
@RequiredArgsConstructor
public class AdminAuditoriaController {

    private final ReportesAuditoriaService servicio;

    @GetMapping("/accesos")
    public ApiResponse<PaginaDto<AccesoAuditoriaDto>> accesos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) ResultadoAcceso resultado,
            @RequestParam(required = false) TipoAcceso tipo,
            @RequestParam(required = false) String periodo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return ApiResponse.ok("Accesos a roles de pago",
                servicio.accesos(new FiltroAccesos(desde, hasta, usuario, resultado, tipo, periodo), pagina, tamano));
    }

    @GetMapping("/acciones")
    public ApiResponse<PaginaDto<AccionAuditoriaDto>> acciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) TipoAccion tipo,
            @RequestParam(required = false) String periodo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return ApiResponse.ok("Acciones sobre roles de pago",
                servicio.acciones(new FiltroAcciones(desde, hasta, usuario, tipo, periodo), pagina, tamano));
    }

    /** Cuentas con intentos denegados. Por defecto, desde 3 intentos. */
    @GetMapping("/denegados")
    public ApiResponse<List<DenegadosPorUsuarioDto>> denegados(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "3") int minimo) {
        return ApiResponse.ok("Intentos denegados por usuario", servicio.denegados(desde, hasta, minimo));
    }

    @GetMapping("/accesos/exportar")
    public ApiResponse<ArchivoDescargaDto> exportarAccesos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) ResultadoAcceso resultado,
            @RequestParam(required = false) TipoAcceso tipo,
            @RequestParam(required = false) String periodo) {
        return ApiResponse.ok("Reporte generado",
                servicio.exportarAccesos(new FiltroAccesos(desde, hasta, usuario, resultado, tipo, periodo)));
    }
}
