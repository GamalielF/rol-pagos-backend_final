package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRolPagoRepository extends BaseRepository<NotificacionRolPago> {

    List<NotificacionRolPago> findByPeriodo_Id(Long idPeriodo);

    /** Los avisos que ya les toca salir, en orden de llegada. El límite lo da el Pageable. */
    @Query("select n from NotificacionRolPago n join fetch n.periodo "
            + "where n.estado = :estado and n.proximoIntento <= :ahora order by n.id")
    List<NotificacionRolPago> listasParaEnviar(@Param("estado") EstadoNotificacion estado,
                                               @Param("ahora") LocalDateTime ahora, Pageable limite);
}
