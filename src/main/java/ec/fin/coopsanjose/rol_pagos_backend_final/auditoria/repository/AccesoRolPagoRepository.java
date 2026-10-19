package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccesoRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AccesoRolPagoRepository extends BaseRepository<AccesoRolPago> {

    /** Intentos denegados agrupados por usuario. */
    interface DenegadosPorUsuario {
        String getUsuario();

        long getIntentos();

        long getCedulasDistintas();

        LocalDateTime getUltimo();
    }

    @Query("select a from AccesoRolPago a "
            + "where a.creadoEn >= :desde and a.creadoEn < :hasta "
            + "and (:usuario is null or a.usernameAd = :usuario) "
            + "and (:resultado is null or a.resultado = :resultado) "
            + "and (:tipo is null or a.tipoAcceso = :tipo) "
            + "and (:periodo is null or a.periodo = :periodo) "
            + "order by a.creadoEn desc, a.id desc")
    Page<AccesoRolPago> buscar(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                               @Param("usuario") String usuario, @Param("resultado") ResultadoAcceso resultado,
                               @Param("tipo") TipoAcceso tipo, @Param("periodo") String periodo,
                               Pageable pageable);

    @Query("select a.usernameAd as usuario, count(a) as intentos, "
            + "count(distinct a.cedulaConsultada) as cedulasDistintas, max(a.creadoEn) as ultimo "
            + "from AccesoRolPago a "
            + "where a.resultado = :denegado and a.creadoEn >= :desde and a.creadoEn < :hasta "
            + "group by a.usernameAd having count(a) >= :minimo "
            + "order by count(a) desc, a.usernameAd")
    List<DenegadosPorUsuario> denegadosPorUsuario(@Param("denegado") ResultadoAcceso denegado,
                                                  @Param("desde") LocalDateTime desde,
                                                  @Param("hasta") LocalDateTime hasta,
                                                  @Param("minimo") long minimo);

    /** Ids de los roles que su titular ya abrió: solo cuenta la consulta propia permitida. */
    @Query("select distinct a.rolPagoId from AccesoRolPago a "
            + "where a.rolPagoId in :ids and a.resultado = :permitido and a.tipoAcceso = :propia")
    List<Long> rolesLeidos(@Param("ids") Collection<Long> ids,
                           @Param("permitido") ResultadoAcceso permitido,
                           @Param("propia") TipoAcceso propia);
}