package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.request.BusquedaDirectorioRequest;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.EstadoDirectorioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.PerfilDirectorioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.ResultadoSincronizacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service.DirectorioAdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/directorio")
@RequiredArgsConstructor
public class AdminDirectorioController {

    private final DirectorioAdService servicio;

    /** Sincroniza ahora mismo con el AD (lista completa). */
    @PostMapping("/sincronizar")
    public ApiResponse<ResultadoSincronizacionDto> sincronizar() {
        return ApiResponse.ok("Directorio sincronizado", servicio.sincronizar());
    }

    /** Cuántos usuarios hay y cómo terminó la última corrida. */
    @GetMapping("/estado")
    public ApiResponse<EstadoDirectorioDto> estado() {
        return ApiResponse.ok("Estado del directorio", servicio.estado());
    }

    /** Búsqueda en vivo en el AD. Va por POST para que la cédula no quede en los logs de acceso. */
    @PostMapping("/buscar")
    public ApiResponse<List<PerfilDirectorioDto>> buscar(@Valid @RequestBody BusquedaDirectorioRequest solicitud) {
        return ApiResponse.ok("Usuarios del AD para la identificación", servicio.buscarEnAd(solicitud.cedula()));
    }

    /** Un usuario de la copia local, con la cédula enmascarada. */
    @GetMapping("/usuarios/{username}")
    public ApiResponse<PerfilDirectorioDto> usuario(@PathVariable String username) {
        return ApiResponse.ok("Usuario del directorio", servicio.buscar(username));
    }
}