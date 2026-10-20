package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.SoporteNotificaciones;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Con app.notificaciones.habilitada=false (el estado de producción hasta que exista la API). */
@TestPropertySource(properties = "app.notificaciones.habilitada=false")
class NotificacionesDeshabilitadasIT extends SoporteNotificaciones {

    @Test
    void publicarNoEncolaNadaPeroPublicaNormalmente() {
        prepararTresPersonas();

        var resultado = publicar();

        assertThat(resultado.estadoPeriodo()).isEqualTo("PUBLICADO");
        assertThat(resultado.avisosEncolados()).isZero();
        assertThat(avisos()).isEmpty();
    }

    @Test
    void generarYReintentarSeNiegan() {
        prepararTresPersonas();
        publicar();

        assertThatThrownBy(() -> notificaciones.generar(PERIODO))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("deshabilitados");
        assertThatThrownBy(() -> notificaciones.reintentar(PERIODO))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("deshabilitados");
    }

    @Test
    void elDespachoNoHaceNada() {
        assertThat(despacho.despacharLote().total()).isZero();

        verify(enviador, never()).enviar(any(), any(), any(), any());
    }
}