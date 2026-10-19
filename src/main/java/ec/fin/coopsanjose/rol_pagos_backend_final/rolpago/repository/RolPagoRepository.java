package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RolPagoRepository extends BaseRepository<RolPago> {

    Optional<RolPago> findByPeriodo_IdAndCedula(Long idPeriodo, String cedula);

    List<RolPago> findByPeriodo_IdOrderByCedulaAsc(Long idPeriodo);

    /** Los roles de una persona en períodos PUBLICADOS, del más reciente al más antiguo. */
    @Query("select r from RolPago r join fetch r.periodo p "
            + "where r.cedula = :cedula and r.estadoRegistro = :estado and p.estadoPeriodo = :publicado "
            + "order by p.fechaPeriodo desc")
    List<RolPago> publicadosDe(@Param("cedula") String cedula,
                               @Param("estado") EstadoRegistro estado,
                               @Param("publicado") EstadoPeriodo publicado);

    @Query("select r from RolPago r join fetch r.periodo where r.id = :id")
    Optional<RolPago> buscarConPeriodo(@Param("id") Long id);
}