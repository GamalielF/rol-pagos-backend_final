package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.scheduler;

import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config.AdProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service.DirectorioAdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sincronización nocturna con el AD. Apagada por defecto: app.ad.sincronizacion.habilitada=false. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SincronizacionProgramada {

    private final DirectorioAdService servicio;
    private final AdProperties propiedades;

    @Scheduled(cron = "${app.ad.sincronizacion.cron:0 0 3 * * *}", zone = "America/Guayaquil")
    public void ejecutar() {
        if (!propiedades.sincronizacion().habilitada()) {
            return;
        }
        try {
            servicio.sincronizar();
        } catch (RuntimeException e) {
            // Si el AD falla, el sistema sigue funcionando con la última copia
            log.error("Sincronización programada fallida: {}", e.getMessage());
        }
    }
}
