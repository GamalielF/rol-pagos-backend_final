package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DirectorioAdRepository extends BaseRepository<DirectorioAd> {

    Optional<DirectorioAd> findByUsernameAd(String usernameAd);

    long countByEstadoRegistro(EstadoRegistro estadoRegistro);

    List<DirectorioAd> findByCedula(String cedula);

    List<DirectorioAd> findByCedulaIn(Collection<String> cedulas);

    List<DirectorioAd> findByUsernameAdIn(Collection<String> usernames);

    /** texto ya viene en minúsculas y con comodines (%texto%), o null para no filtrar. */
    @Query("select d from DirectorioAd d where d.estadoRegistro = :estado "
            + "and (:texto is null or lower(d.usernameAd) like :texto or lower(d.nombreCompleto) like :texto) "
            + "order by d.nombreCompleto, d.usernameAd")
    Page<DirectorioAd> buscar(@Param("estado") EstadoRegistro estado, @Param("texto") String texto, Pageable pageable);
}