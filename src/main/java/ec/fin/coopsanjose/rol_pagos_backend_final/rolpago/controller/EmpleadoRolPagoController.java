package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioActual;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.request.ConsultaRolPagoRequest;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoPdfDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.ConsultaRolPagoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/** La ventana del empleado: SOLO su propio rol, sin importar qué cédula conozca. */
@RestController
@RequestMapping("/mi-rol-pago")
@RequiredArgsConstructor
public class EmpleadoRolPagoController {

    private final ConsultaRolPagoService servicio;

    /** POST para que la cédula no quede en los registros de acceso de los servidores. */
    @PostMapping("/consultar")
    public ApiResponse<RolPagoPdfDto> consultar(@Valid @RequestBody ConsultaRolPagoRequest solicitud,
                                                HttpServletRequest http) {
        YearMonth periodo = solicitud.periodo() == null ? null : YearMonth.parse(solicitud.periodo());
        return ApiResponse.ok("Rol de pago",
                servicio.consultarPropio(sesion(), solicitud.cedula(), periodo, http.getRemoteAddr()));
    }

    @GetMapping("/periodos")
    public ApiResponse<List<String>> periodos() {
        return ApiResponse.ok("Períodos disponibles", servicio.listarPeriodosPropios(sesion()));
    }

    private static UsuarioAutenticado sesion() {
        return UsuarioActual.obtener().orElseThrow(() -> new IllegalStateException("No hay una sesión autenticada"));
    }
}
