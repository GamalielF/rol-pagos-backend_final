package ec.fin.coopsanjose.rol_pagos_backend_final.common.prueba;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.OperacionNoPermitidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** SOLO PARA DESARROLLO: permite ver el formato de cada tipo de respuesta. Se elimina antes de producción. */
@Profile("dev")
@RestController
@RequestMapping("/prueba")
public class PruebaFormatoController {

    public record SolicitudPrueba(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser al menos 1") Integer cantidad) {
    }

    @GetMapping("/ok")
    public ApiResponse<Map<String, Object>> ok() {
        Map<String, Object> datos = Map.of("periodo", "2026-10", "total", 3);
        return ApiResponse.ok("Respuesta exitosa de prueba", datos);
    }

    @GetMapping("/no-encontrado")
    public ApiResponse<Void> noEncontrado() {
        throw new RecursoNoEncontradoException("El periodo 2099-01 no existe");
    }

    @GetMapping("/conflicto")
    public ApiResponse<Void> conflicto() {
        throw new ConflictoException("El periodo 2026-10 ya fue publicado");
    }

    @GetMapping("/no-permitido")
    public ApiResponse<Void> noPermitido() {
        throw new OperacionNoPermitidaException("Los registros no se eliminan por razones de auditoria");
    }

    @GetMapping("/bloqueo-optimista")
    public ApiResponse<Void> bloqueoOptimista() {
        throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
    }

    @GetMapping("/error-interno")
    public ApiResponse<Void> errorInterno() {
        throw new IllegalStateException("detalle-interno-secreto");
    }

    @GetMapping("/numero/{valor}")
    public ApiResponse<Integer> numero(@PathVariable int valor) {
        return ApiResponse.ok("Numero recibido", valor);
    }

    @PostMapping("/validar")
    public ApiResponse<SolicitudPrueba> validar(@Valid @RequestBody SolicitudPrueba solicitud) {
        return ApiResponse.ok("Solicitud valida", solicitud);
    }
}