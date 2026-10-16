package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;

import java.util.List;
import java.util.Optional;

public interface RolPagoRepository extends BaseRepository<RolPago> {

    Optional<RolPago> findByPeriodo_IdAndUsernameAd(Long idPeriodo, String usernameAd);

    List<RolPago> findByPeriodo_Id(Long idPeriodo);

    long countByPeriodo_IdAndEstadoRegistro(Long idPeriodo, EstadoRegistro estadoRegistro);
}
