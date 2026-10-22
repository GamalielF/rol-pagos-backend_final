package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreo;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreoConVariables;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DespachoNotificacionesEntregaTest {

    private static final Map<String, String> VARIABLES = Map.of("Mes", "septiembre de 2026");

    @Test
    void unEnviadorSinVariablesRecibeLaFormaSimple() {
        EnviadorCorreo simple = mock(EnviadorCorreo.class);
        DespachoNotificaciones.entregar(simple, "a@b.test", "Asunto", "Texto", "aviso-1", VARIABLES);
        verify(simple).enviar("a@b.test", "Asunto", "Texto", "aviso-1");
    }

    @Test
    void unEnviadorConVariablesLasRecibeYNoSeUsaLaFormaSimple() {
        EnviadorCorreoConVariables completo = mock(EnviadorCorreoConVariables.class);
        DespachoNotificaciones.entregar(completo, "a@b.test", "Asunto", "Texto", "aviso-1", VARIABLES);
        verify(completo).enviar("a@b.test", "Asunto", "Texto", "aviso-1", VARIABLES);
        verify(completo, never()).enviar(any(), any(), any(), any());
    }
}