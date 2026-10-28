package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.InformeNotificacionesDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ResultadoEncoladoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ResultadoReenvioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ResultadoReintentoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service.NotificacionService;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service.ReenvioNotificacionService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller.PeriodoPath;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rrhh/periodos/{periodo}/notificaciones")
@RequiredArgsConstructor
public class RrhhNotificacionController {

    private final NotificacionService servicio;
    private final ReenvioNotificacionService reenvio;

    /** Cuántos avisos salieron, cuántos esperan, y a quién no se le pudo avisar. */
    @GetMapping
    public ApiResponse<InformeNotificacionesDto> informe(@PathVariable String periodo) {
        return ApiResponse.ok("Avisos del período", servicio.informe(PeriodoPath.parsear(periodo)));
    }

    /** Encola los avisos que falten (por ejemplo, si estaban deshabilitados al publicar). Es idempotente. */
    @PostMapping("/generar")
    public ApiResponse<ResultadoEncoladoDto> generar(@PathVariable String periodo) {
        return ApiResponse.ok("Avisos generados", servicio.generar(PeriodoPath.parsear(periodo)));
    }

    /** Reenvía a mano el aviso de UN rol, con la API de correo individual. El resultado queda en el aviso. */
    @PostMapping("/{rolPagoId}/reenviar")
    public ApiResponse<ResultadoReenvioDto> reenviar(@PathVariable String periodo, @PathVariable long rolPagoId) {
        ResultadoReenvioDto resultado = reenvio.reenviar(PeriodoPath.parsear(periodo), rolPagoId);
        return ApiResponse.ok(resultado.aceptado() ? "Aviso reenviado" : "No se pudo reenviar el aviso", resultado);
    }

    /** Devuelve a la cola los avisos que fallaron. */
    @PostMapping("/reintentar")
    public ApiResponse<ResultadoReintentoDto> reintentar(@PathVariable String periodo) {
        return ApiResponse.ok("Avisos devueltos a la cola",
                new ResultadoReintentoDto(servicio.reintentar(PeriodoPath.parsear(periodo))));
    }
}
