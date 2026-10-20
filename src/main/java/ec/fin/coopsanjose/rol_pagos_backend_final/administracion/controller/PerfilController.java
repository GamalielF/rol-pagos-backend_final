package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response.PerfilDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioActual;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.util.Enmascarador;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mi-perfil")
public class PerfilController {

    /** Quién soy y qué módulos me corresponden. La cédula nunca sale completa. */
    @GetMapping
    public ApiResponse<PerfilDto> perfil() {
        UsuarioAutenticado u = UsuarioActual.obtener()
                .orElseThrow(() -> new IllegalStateException("No hay una sesión autenticada"));
        return ApiResponse.ok("Perfil del usuario", new PerfilDto(u.username(), u.nombre(),
                Enmascarador.cedula(u.cedula()), u.cedula() != null && !u.cedula().isBlank(),
                u.roles().stream().map(Enum::name).sorted().toList(),
                u.permisos().stream().map(Enum::name).sorted().toList()));
    }
}