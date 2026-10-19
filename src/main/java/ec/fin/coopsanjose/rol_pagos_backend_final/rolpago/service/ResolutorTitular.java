package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service.DirectorioAdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Averigua quién es el titular de una cédula según el AD. Un rol es de una persona, sin importar cuántos códigos tenga. */
@Component
@RequiredArgsConstructor
public class ResolutorTitular {

    private final DirectorioAdRepository directorioRepository;
    private final DirectorioAdService directorioService;

    /**
     * Registros ACTIVOS del directorio para esa cédula. Si no hay ninguno, consulta al AD en vivo y vuelve a mirar.
     * @throws ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ServicioExternoException si el AD falla
     */
    public List<DirectorioAd> activosPorCedula(String cedula) {
        List<DirectorioAd> activos = activos(cedula);
        if (!activos.isEmpty()) {
            return activos;
        }
        try {
            directorioService.buscarEnAd(cedula);   // actualiza la copia local
        } catch (RecursoNoEncontradoException e) {
            // El AD no tiene a nadie utilizable con esa identificación
        }
        return activos(cedula);
    }

    /** ¿Hay alguien con esa cédula, pero todos inactivos? Sirve para dar un motivo preciso. */
    public boolean existeSoloInactivos(String cedula) {
        return !directorioRepository.findByCedula(cedula).isEmpty();
    }

    private List<DirectorioAd> activos(String cedula) {
        return directorioRepository.findByCedula(cedula).stream()
                .filter(d -> d.getEstadoRegistro() == EstadoRegistro.ACTIVO)
                .toList();
    }
}
