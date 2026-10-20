package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.entity.AsignacionRol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsignacionRolRepository extends BaseRepository<AsignacionRol> {

    Optional<AsignacionRol> findByUsernameAdAndRol(String usernameAd, Rol rol);

    List<AsignacionRol> findByUsernameAdAndEstadoRegistro(String usernameAd, EstadoRegistro estadoRegistro);

    List<AsignacionRol> findByUsernameAdInAndEstadoRegistro(Collection<String> usernames, EstadoRegistro estadoRegistro);

    List<AsignacionRol> findByEstadoRegistro(EstadoRegistro estadoRegistro);
}