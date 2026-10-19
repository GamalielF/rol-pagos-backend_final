package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DirectorioAdRepository extends BaseRepository<DirectorioAd> {

    Optional<DirectorioAd> findByUsernameAd(String usernameAd);

    long countByEstadoRegistro(EstadoRegistro estadoRegistro);

    List<DirectorioAd> findByCedula(String cedula);

    List<DirectorioAd> findByCedulaIn(Collection<String> cedulas);
}