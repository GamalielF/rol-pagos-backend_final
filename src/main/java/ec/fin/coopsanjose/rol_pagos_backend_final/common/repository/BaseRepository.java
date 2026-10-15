package ec.fin.coopsanjose.rol_pagos_backend_final.common.repository;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.BaseEntity;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Capa 2 contra el borrado: solo expone operaciones seguras, sin ningún delete*. */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends Repository<T, Long> {

    <S extends T> S save(S entidad);

    Optional<T> findById(Long id);

    boolean existsById(Long id);

    List<T> findAll();

    Page<T> findAll(Pageable pageable);

    long count();

    List<T> findAllByEstadoRegistro(EstadoRegistro estadoRegistro);
}
