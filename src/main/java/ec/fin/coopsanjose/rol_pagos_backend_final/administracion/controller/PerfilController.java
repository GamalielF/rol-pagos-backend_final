package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response.PerfilDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.service.PerfilService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioActual;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mi-perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService servicio;

    /** Quién soy y qué módulos me corresponden. La cédula nunca sale completa. */
    @GetMapping
    public ApiResponse<PerfilDto> perfil() {
        UsuarioAutenticado usuario = UsuarioActual.obtener()
                .orElseThrow(() -> new IllegalStateException("No hay una sesión autenticada"));
        return ApiResponse.ok("Perfil del usuario", servicio.perfilDe(usuario));
    }
}