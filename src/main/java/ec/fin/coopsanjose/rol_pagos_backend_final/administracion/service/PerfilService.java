package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response.PerfilDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.util.Enmascarador;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final DirectorioAdRepository directorioRepository;

    @Transactional(readOnly = true)
    public PerfilDto perfilDe(UsuarioAutenticado usuario) {
        String cargo = directorioRepository.findByUsernameAd(usuario.username())
                .map(DirectorioAd::getCargo).orElse(null);
        return new PerfilDto(usuario.username(), usuario.nombre(), cargo, Enmascarador.cedula(usuario.cedula()),
                usuario.cedula() != null && !usuario.cedula().isBlank(),
                usuario.roles().stream().sorted(Comparator.comparingInt(Rol::ordinal)).map(Enum::name).toList(),
                usuario.permisos().stream().map(Enum::name).sorted().toList());
    }
}