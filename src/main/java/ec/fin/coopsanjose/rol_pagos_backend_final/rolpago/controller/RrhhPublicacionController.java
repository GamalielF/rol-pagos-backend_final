package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioActual;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.request.MotivoConsultaRequest;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.request.PublicarPeriodoRequest;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PublicacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResumenPeriodoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoPdfDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.ConsultaRolPagoService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.PublicacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rrhh/periodos/{periodo}")
@RequiredArgsConstructor
public class RrhhPublicacionController {

    private final PublicacionService publicacionService;
    private final ConsultaRolPagoService consultaService;

    /** Qué hay cargado y quién falta, antes de publicar. */
    @GetMapping("/resumen")
    public ApiResponse<ResumenPeriodoDto> resumen(@PathVariable String periodo) {
        return ApiResponse.ok("Resumen del período", publicacionService.resumen(PeriodoPath.parsear(periodo)));
    }

    /** Sin cuerpo, o con {"confirmarFaltantes": true} si hay personas activas sin rol. */
    @PostMapping("/publicar")
    public ApiResponse<PublicacionDto> publicar(@PathVariable String periodo,
                                                @RequestBody(required = false) PublicarPeriodoRequest solicitud) {
        boolean confirmar = solicitud != null && Boolean.TRUE.equals(solicitud.confirmarFaltantes());
        return ApiResponse.ok("Período publicado",
                publicacionService.publicar(PeriodoPath.parsear(periodo), confirmar, sesion().username()));
    }

    /** Ver el rol de otra persona (o previsualizar antes de publicar): motivo obligatorio y queda registrado. */
    @PostMapping("/roles-pago/{id}/ver")
    public ApiResponse<RolPagoPdfDto> ver(@PathVariable String periodo, @PathVariable Long id,
                                          @Valid @RequestBody MotivoConsultaRequest solicitud,
                                          HttpServletRequest http) {
        return ApiResponse.ok("Rol de pago", consultaService.verComoPersonal(
                sesion(), PeriodoPath.parsear(periodo), id, solicitud.motivo(), http.getRemoteAddr()));
    }

    private static UsuarioAutenticado sesion() {
        return UsuarioActual.obtener().orElseThrow(() -> new IllegalStateException("No hay una sesión autenticada"));
    }
}
