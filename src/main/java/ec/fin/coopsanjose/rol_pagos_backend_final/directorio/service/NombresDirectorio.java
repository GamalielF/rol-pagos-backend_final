package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/** Un nombre por cédula, para mostrar en reportes. Prefiere el registro ACTIVO. */
@Component
@RequiredArgsConstructor
public class NombresDirectorio {

    private final DirectorioAdRepository repositorio;

    public Map<String, String> porCedula(Collection<String> cedulas) {
        Map<String, String> nombres = new HashMap<>();
        if (cedulas.isEmpty()) {
            return nombres;
        }
        repositorio.findByCedulaIn(cedulas).stream()
                .sorted(Comparator.comparing((DirectorioAd d) -> d.getEstadoRegistro() != EstadoRegistro.ACTIVO)
                        .thenComparing(DirectorioAd::getUsernameAd))
                .forEach(d -> nombres.putIfAbsent(d.getCedula(), d.getNombreCompleto()));
        return nombres;
    }
}
