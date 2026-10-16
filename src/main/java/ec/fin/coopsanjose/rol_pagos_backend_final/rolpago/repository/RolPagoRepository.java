package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;

import java.util.Collection;

import java.util.List;
import java.util.Optional;

public interface RolPagoRepository extends BaseRepository<RolPago> {

    Optional<RolPago> findByPeriodo_IdAndCedula(Long idPeriodo, String cedula);

    List<RolPago> findByPeriodo_IdOrderByCedulaAsc(Long idPeriodo);
    List<DirectorioAd> findByCedulaIn(Collection<String> cedulas);
}