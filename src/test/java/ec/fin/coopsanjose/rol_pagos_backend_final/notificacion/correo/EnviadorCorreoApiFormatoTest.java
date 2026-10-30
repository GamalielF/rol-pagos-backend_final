package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.EnvioCorreoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Lo que el adaptador manda a la API y cómo lee lo que ella responde. */
class EnviadorCorreoApiFormatoTest {

    private static final String URL = "https://correo.interno.test/api/enviar";
    private static final String DESTINO = "juan.perez@coopsanjose.fin.ec";

    private MockRestServiceServer servidor;
    private RestClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = constructor.build();
    }

    private EnviadorCorreoApi enviador(EnvioCorreoProperties envio) {
        return new EnviadorCorreoApi(new CorreoProperties.Api(URL, "secreto", "Authorization", "Bearer", null,
                Duration.ofSeconds(5), Duration.ofSeconds(15), null), cliente, envio);
    }

    private void enviar(EnviadorCorreoApi enviador) {
        enviador.enviar(DESTINO, "Asunto del aviso", "Texto del aviso", "aviso-7");
    }

    @Test
    void elCuerpoLlevaUnSoloAvisoEnLaListaNotifications() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.notifications", hasSize(1)))
                .andExpect(jsonPath("$.notifications[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$.notifications[0].destinatario").value(DESTINO))
                .andExpect(jsonPath("$.notifications[0].asunto").value("Asunto del aviso"))
                .andExpect(jsonPath("$.notifications[0].mensaje").value("Texto del aviso"))
                .andRespond(withSuccess());
        enviar(enviador(EnvioCorreoProperties.SIN_AJUSTES));
        servidor.verify();
    }

    @Test
    void sinPlantillaNoSeEnvianNiElCodigoNiLasVariables() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.notifications[0].codigoPlantilla").doesNotExist())
                .andExpect(jsonPath("$.notifications[0].variables").doesNotExist())
                .andRespond(withSuccess());
        enviar(enviador(EnvioCorreoProperties.SIN_AJUSTES));
        servidor.verify();
    }

    @Test
    void conPlantillaSeEnvianElCodigoYLasVariablesFijas() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.notifications[0].codigoPlantilla").value("ROL_PAGO_DISPONIBLE"))
                .andExpect(jsonPath("$.notifications[0].variables.NombreEmpresa").value("COOPSANJOSE LTDA"))
                .andRespond(withSuccess());
        enviar(enviador(new EnvioCorreoProperties("EMAIL", "ROL_PAGO_DISPONIBLE",
                Map.of("NombreEmpresa", "COOPSANJOSE LTDA"), null)));
        servidor.verify();
    }

    @Test
    void laRedireccionSoloCambiaElDestinoYNoTocaNiElAsuntoNiElCuerpoNiFiltraLaDireccionReal() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.notifications[0].destinatario").value("prueba@coopsanjose.fin.ec"))
                .andExpect(jsonPath("$.notifications[0].asunto").value("Asunto del aviso"))
                .andExpect(jsonPath("$.notifications[0].mensaje").value("Texto del aviso"))
                .andExpect(content().string(not(containsString(DESTINO))))
                .andRespond(withSuccess());
        enviar(enviador(new EnvioCorreoProperties(null, null, null, "prueba@coopsanjose.fin.ec")));
        servidor.verify();
    }

    @Test
    void conUnaUrlMalEscritaElMensajeDiceQueTieneMalSinRepetirlaYNoLlamaALaApi() {
        EnviadorCorreoApi enviador = new EnviadorCorreoApi(new CorreoProperties.Api("\"https://correo.secreto.test/api?k=ABC\"", "secreto", "Authorization", "Bearer", null,
                Duration.ofSeconds(5), Duration.ofSeconds(15), null), RestClient.builder().build(), EnvioCorreoProperties.SIN_AJUSTES);

        assertThatThrownBy(() -> enviador.enviar(DESTINO, "Asunto", "Texto", "aviso-7"))
                .isInstanceOfSatisfying(EnvioCorreoException.class, e -> {
                    assertThat(e.getMessage()).contains("comillas").doesNotContain("secreto").doesNotContain("ABC");
                    assertThat(e.esReintentable()).isTrue();
                });
    }

    @Test
    void conUnTokenConSaltoDeLineaElMensajeLoSeñalaSinRepetirlo() {
        EnviadorCorreoApi enviador = new EnviadorCorreoApi(new CorreoProperties.Api(URL, "clave-privada\n", "Authorization", "Bearer", null,
                Duration.ofSeconds(5), Duration.ofSeconds(15), null), RestClient.builder().build(), EnvioCorreoProperties.SIN_AJUSTES);

        assertThatThrownBy(() -> enviador.enviar(DESTINO, "Asunto", "Texto", "aviso-7"))
                .isInstanceOfSatisfying(EnvioCorreoException.class,
                        e -> assertThat(e.getMessage()).contains("token").contains("caracteres no permitidos").doesNotContain("clave-privada"));
    }

    @Test
    void unErrorInesperadoAlPrepararElEnvioSeConvierteEnUnFalloVisibleYReintentable() {
        RestClient quebrado = RestClient.builder()
                .requestFactory((uri, metodo) -> {
                    throw new IllegalStateException("fallo raro con datos internos");
                })
                .build();
        EnviadorCorreoApi enviador = new EnviadorCorreoApi(new CorreoProperties.Api(URL, "secreto", "Authorization", "Bearer", null,
                Duration.ofSeconds(5), Duration.ofSeconds(15), null), quebrado, EnvioCorreoProperties.SIN_AJUSTES);

        assertThatThrownBy(() -> enviador.enviar(DESTINO, "Asunto", "Texto", "aviso-7"))
                .isInstanceOfSatisfying(EnvioCorreoException.class, e -> {
                    assertThat(e.esReintentable()).isTrue();
                    assertThat(e.getMessage()).doesNotContain("datos internos");
                });
    }

    @Test
    void unaRespuestaConSuccessFalseSeMarcaParaReintentar() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> enviar(enviador(EnvioCorreoProperties.SIN_AJUSTES)))
                .isInstanceOfSatisfying(EnvioCorreoException.class, e -> assertThat(e.esReintentable()).isTrue());
    }

    @Test
    void unaRespuestaExitosaOCualquierOtroTextoSeDaPorAceptada() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"success\":true,\"status\":\"PROCESSING\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL)).andRespond(withSuccess("OK, recibido", MediaType.TEXT_PLAIN));
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"sin\":\"campo success\"}", MediaType.APPLICATION_JSON));
        EnviadorCorreoApi enviador = enviador(EnvioCorreoProperties.SIN_AJUSTES);
        assertThatCode(() -> {
            enviar(enviador);
            enviar(enviador);
            enviar(enviador);
        }).doesNotThrowAnyException();
    }

    @Test
    void elMensajeDeErrorNoRepiteLoQueDiceLaApi() {
        servidor.expect(requestTo(URL)).andRespond(
                withSuccess("{\"success\":false,\"message\":\"fallo para " + DESTINO + "\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> enviar(enviador(EnvioCorreoProperties.SIN_AJUSTES)))
                .isInstanceOf(EnvioCorreoException.class)
                .message().doesNotContain(DESTINO).doesNotContain("fallo para");
    }
}