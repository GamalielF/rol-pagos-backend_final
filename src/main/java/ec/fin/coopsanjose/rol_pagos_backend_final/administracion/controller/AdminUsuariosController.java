package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.request.OtorgarRolRequest;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response.UsuarioAdminDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.service.AdministracionUsuariosService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.PaginaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Se usa POST para otorgar y revocar: así funciona con la configuración de CORS (GET, POST). */
@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuariosController {

    private final AdministracionUsuariosService servicio;

    @GetMapping
    public ApiResponse<PaginaDto<UsuarioAdminDto>> buscar(@RequestParam(required = false) String buscar,
                                                          @RequestParam(defaultValue = "0") int pagina,
                                                          @RequestParam(defaultValue = "50") int tamano) {
        return ApiResponse.ok("Usuarios del directorio", servicio.buscar(buscar, pagina, tamano));
    }

    @GetMapping("/privilegiados")
    public ApiResponse<List<UsuarioAdminDto>> privilegiados() {
        return ApiResponse.ok("Usuarios con rol ADMIN o RRHH", servicio.privilegiados());
    }

    /** Cuerpo opcional: {"verRolesAjenos": false} para dar RRHH sin el permiso de abrir roles ajenos. */
    @PostMapping("/{username}/roles/{rol}/otorgar")
    public ApiResponse<UsuarioAdminDto> otorgar(@PathVariable String username, @PathVariable Rol rol,
                                                @RequestBody(required = false) OtorgarRolRequest solicitud) {
        Boolean ver = solicitud == null ? null : solicitud.verRolesAjenos();
        return ApiResponse.ok("Rol otorgado", servicio.otorgar(username, rol, ver));
    }

    @PostMapping("/{username}/roles/{rol}/revocar")
    public ApiResponse<UsuarioAdminDto> revocar(@PathVariable String username, @PathVariable Rol rol) {
        return ApiResponse.ok("Rol revocado", servicio.revocar(username, rol));
    }
}