package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import java.util.Optional;

/** Fuente confiable de identidad. Hoy la respalda la copia local del AD. */
public interface DirectorioUsuarios {

    Optional<PerfilDirectorio> buscarPorUsername(String username);
}
