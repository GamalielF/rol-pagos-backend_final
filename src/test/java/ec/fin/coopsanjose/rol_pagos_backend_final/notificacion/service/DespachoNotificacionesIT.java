package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.SoporteNotificaciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnvioCorreoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service.DespachoNotificaciones.ResultadoLote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DespachoNotificacionesIT extends SoporteNotificaciones {

    private static final EnvioCorreoException CAIDA = new EnvioCorreoException("No se pudo conectar con la API de correo", true, null);

    @BeforeEach
    void escenario() {
        prepararTresPersonas();
        publicar();
    }

    private NotificacionRolPago elPrimeroEnSalir() {
        return avisos().stream().min(Comparator.comparing(NotificacionRolPago::getId)).orElseThrow();
    }

    @Test
    void cadaAvisoSaleUnaSolaVezYSinDatosDeNomina() {
        ResultadoLote primero = despacho.despacharLote();
        ResultadoLote segundo = despacho.despacharLote();

        assertThat(primero.enviadas()).isEqualTo(3);
        assertThat(segundo.total()).isZero();   // lo ya enviado no se reenvía
        ArgumentCaptor<String> destinos = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> asuntos = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cuerpos = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> referencias = ArgumentCaptor.forClass(String.class);
        verify(enviador, times(3)).enviar(destinos.capture(), asuntos.capture(), cuerpos.capture(), referencias.capture());
        assertThat(destinos.getAllValues()).containsExactlyInAnyOrder(
                "zz.juan@coopsanjose.fin.ec", "zz.maria@coopsanjose.fin.ec", "zz.luis@coopsanjose.fin.ec");
        assertThat(asuntos.getAllValues()).containsOnly("Su rol de pago de marzo de 2099 está disponible");
        assertThat(cuerpos.getAllValues()).allSatisfy(c -> assertThat(c).contains("marzo de 2099")
                .doesNotContain(NOMBRE_JUAN).doesNotContain(NOMBRE_MARIA).doesNotContain(NOMBRE_LUIS)
                .doesNotContain(CEDULA_JUAN).doesNotContain(CEDULA_MARIA).doesNotContain(CEDULA_LUIS));
        assertThat(referencias.getAllValues()).allMatch(r -> r.matches("aviso-\\d+")).doesNotHaveDuplicates();
        assertThat(avisos()).allSatisfy(a -> {
            assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
            assertThat(a.getIntentos()).isEqualTo(1);
            assertThat(a.getEnviadaEn()).isNotNull();
            assertThat(a.getProximoIntento()).isNull();
            assertThat(a.getUltimoError()).isNull();
        });
    }

    @Test
    void sinElServicioConfiguradoLosAvisosEsperanIntactos() {
        when(enviador.estaConfigurado()).thenReturn(false);

        assertThat(despacho.despacharLote().total()).isZero();

        verify(enviador, never()).enviar(any(), any(), any(), any());
        assertThat(avisos()).allSatisfy(a -> {
            assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
            assertThat(a.getIntentos()).isZero();
        });
    }

    @Test
    void unFalloTemporalSeReintentaMasTardeYDetieneElLote() {
        doThrow(CAIDA).when(enviador).enviar(any(), any(), any(), any());
        LocalDateTime antes = LocalDateTime.now(clock);

        ResultadoLote resultado = despacho.despacharLote();

        assertThat(resultado.reintentos()).isEqualTo(1);
        assertThat(resultado.enviadas()).isZero();
        verify(enviador, times(1)).enviar(any(), any(), any(), any());   // no se insistió con el resto
        List<NotificacionRolPago> avisos = avisos();
        assertThat(avisos).filteredOn(a -> a.getIntentos() == 1).singleElement().satisfies(a -> {
            assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
            assertThat(a.getUltimoError()).isEqualTo("No se pudo conectar con la API de correo");
            assertThat(a.getProximoIntento()).isAfter(antes.plusSeconds(30)).isBefore(antes.plusMinutes(2));   // ~1 minuto
        });
        assertThat(avisos).filteredOn(a -> a.getIntentos() == 0).hasSize(2);
    }

    @Test
    void laEsperaSeDuplicaEnElSegundoFallo() {
        doThrow(CAIDA).when(enviador).enviar(any(), any(), any(), any());
        despacho.despacharLote();
        NotificacionRolPago fallado = avisos().stream().filter(a -> a.getIntentos() == 1).findFirst().orElseThrow();
        fallado.setProximoIntento(LocalDateTime.now(clock).minusSeconds(1));   // simula que ya pasó la espera
        avisoRepository.save(fallado);
        em.flush();
        LocalDateTime antes = LocalDateTime.now(clock);

        despacho.despacharLote();

        assertThat(avisos().stream().filter(a -> a.getId().equals(fallado.getId())).findFirst().orElseThrow())
                .satisfies(a -> {
                    assertThat(a.getIntentos()).isEqualTo(2);
                    assertThat(a.getProximoIntento()).isAfter(antes.plusSeconds(90)).isBefore(antes.plusMinutes(3));   // ~2 minutos
                });
    }

    @Test
    void alAgotarLosIntentosElAvisoQuedaFallido() {
        NotificacionRolPago ultimo = elPrimeroEnSalir();
        ultimo.setIntentos(4);   // el máximo es 5
        avisoRepository.save(ultimo);
        em.flush();
        doThrow(CAIDA).when(enviador).enviar(any(), any(), any(), any());

        ResultadoLote resultado = despacho.despacharLote();

        assertThat(resultado.fallidas()).isEqualTo(1);
        assertThat(avisos().stream().filter(a -> a.getId().equals(ultimo.getId())).findFirst().orElseThrow())
                .satisfies(a -> {
                    assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
                    assertThat(a.getIntentos()).isEqualTo(5);
                    assertThat(a.getProximoIntento()).isNull();
                });
    }

    @Test
    void unaDireccionRechazadaEsDefinitivaYNoDetieneElResto() {
        doThrow(new EnvioCorreoException("La API de correo rechazó la solicitud (HTTP 422)", false, null))
                .when(enviador).enviar(eq("zz.maria@coopsanjose.fin.ec"), any(), any(), any());

        ResultadoLote resultado = despacho.despacharLote();

        assertThat(resultado.enviadas()).isEqualTo(2);
        assertThat(resultado.fallidas()).isEqualTo(1);
        assertThat(avisoDe(CEDULA_MARIA)).satisfies(a -> {
            assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
            assertThat(a.getIntentos()).isEqualTo(1);
            assertThat(a.getProximoIntento()).isNull();
        });
        assertThat(avisoDe(CEDULA_JUAN).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(avisoDe(CEDULA_LUIS).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    @Test
    void unAvisoEnEsperaNoSaleAntesDeSuHora() {
        NotificacionRolPago esperando = avisoDe(CEDULA_MARIA);
        esperando.setProximoIntento(LocalDateTime.now(clock).plusMinutes(10));
        avisoRepository.save(esperando);
        em.flush();

        ResultadoLote resultado = despacho.despacharLote();

        assertThat(resultado.enviadas()).isEqualTo(2);
        verify(enviador, never()).enviar(eq("zz.maria@coopsanjose.fin.ec"), any(), any(), any());
        assertThat(avisoDe(CEDULA_MARIA).getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
    }
}