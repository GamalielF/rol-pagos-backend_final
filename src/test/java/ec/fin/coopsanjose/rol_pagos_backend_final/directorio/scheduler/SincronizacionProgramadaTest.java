package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.scheduler;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ServicioExternoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config.AdProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service.DirectorioAdService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SincronizacionProgramadaTest {

    private final DirectorioAdService servicio = mock(DirectorioAdService.class);

    private SincronizacionProgramada tarea(boolean habilitada) {
        AdProperties propiedades = new AdProperties("http://ad.test", "", null, Duration.ofSeconds(5),
                Duration.ofSeconds(30), "coopsanjose.fin.ec", List.of(),
                new AdProperties.Sincronizacion(habilitada, "0 0 3 * * *", 50));
        return new SincronizacionProgramada(servicio, propiedades);
    }

    @Test
    void apagadaNoSincroniza() {
        tarea(false).ejecutar();

        verifyNoInteractions(servicio);
    }

    @Test
    void encendidaSincronizaUnaVez() {
        tarea(true).ejecutar();

        verify(servicio, times(1)).sincronizar();
    }

    @Test
    void unFalloDelAdNoSePropaga() {
        when(servicio.sincronizar()).thenThrow(new ServicioExternoException("El AD no respondió a tiempo"));

        assertThatCode(() -> tarea(true).ejecutar()).doesNotThrowAnyException();
    }
}