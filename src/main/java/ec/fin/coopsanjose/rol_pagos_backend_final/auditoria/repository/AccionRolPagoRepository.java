package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AccionRolPagoRepository extends BaseRepository<AccionRolPago> {

    @Query("select a from AccionRolPago a "
            + "where a.creadoEn >= :desde and a.creadoEn < :hasta "
            + "and (:usuario is null or a.usernameAd = :usuario) "
            + "and (:tipo is null or a.tipoAccion = :tipo) "
            + "and (:periodo is null or a.periodo = :periodo) "
            + "order by a.creadoEn desc, a.id desc")
    Page<AccionRolPago> buscar(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                               @Param("usuario") String usuario, @Param("tipo") TipoAccion tipo,
                               @Param("periodo") String periodo, Pageable pageable);
}
