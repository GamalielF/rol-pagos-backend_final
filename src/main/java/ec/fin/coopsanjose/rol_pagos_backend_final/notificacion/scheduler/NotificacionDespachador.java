package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.scheduler;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.NotificacionProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service.DespachoNotificaciones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Revisa la cola cada app.notificaciones.intervalo-ms. La primera revisión espera un intervalo completo. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionDespachador {

    private final DespachoNotificaciones despacho;
    private final NotificacionProperties propiedades;

    @Scheduled(initialDelayString = "${app.notificaciones.intervalo-ms:30000}",
            fixedDelayString = "${app.notificaciones.intervalo-ms:30000}")
    public void ejecutar() {
        if (!propiedades.habilitada() || !propiedades.despachoAutomatico()) {
            return;
        }
        try {
            DespachoNotificaciones.ResultadoLote resultado = despacho.despacharLote();
            if (resultado.total() > 0) {
                log.info("Avisos: {} enviados, {} en reintento, {} fallidos",
                        resultado.enviadas(), resultado.reintentos(), resultado.fallidas());
            }
        } catch (RuntimeException e) {
            log.error("Fallo inesperado en el despacho de avisos", e);
        }
    }
}
