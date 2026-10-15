package ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.repository.BaseRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PeriodoRepository extends BaseRepository<Periodo> {

    Optional<Periodo> findByFechaPeriodo(LocalDate fechaPeriodo);
}