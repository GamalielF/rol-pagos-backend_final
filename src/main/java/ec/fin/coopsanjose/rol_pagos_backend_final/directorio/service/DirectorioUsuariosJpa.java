package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.DirectorioUsuarios;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.PerfilDirectorio;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DirectorioUsuariosJpa implements DirectorioUsuarios {

    private final DirectorioAdRepository repositorio;

    @Override
    @Transactional(readOnly = true)
    public Optional<PerfilDirectorio> buscarPorUsername(String username) {
        return repositorio.findByUsernameAd(username)
                .map(d -> new PerfilDirectorio(d.getUsernameAd(), d.getCedula(), d.getNombreCompleto(),
                        d.getEmail(), d.getEstadoRegistro() == EstadoRegistro.ACTIVO));
    }
}
