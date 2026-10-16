package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.SincronizacionAd;

import java.util.Optional;

public interface SincronizacionAdRepository extends BaseRepository<SincronizacionAd> {

    Optional<SincronizacionAd> findFirstByOrderByInicioDesc();
}
