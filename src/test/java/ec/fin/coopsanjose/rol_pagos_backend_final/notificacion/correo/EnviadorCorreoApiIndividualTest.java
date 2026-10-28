package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoIndividualProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.EnvioCorreoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** El reenvío de un aviso a una sola persona con la API individual de correo. */
class EnviadorCorreoApiIndividualTest {

    private static final String URL_MASIVA = "https://correo.interno.test/api/enviar";
    private static final String URL_INDIVIDUAL = "https://correo.interno.test/api/send-prueba";
    private static final String DESTINO = "fsepa@coopsanjose.fin.ec";

    private MockRestServiceServer servidor;
    private RestClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = constructor.build();
    }

    private EnviadorCorreoApi enviador(CorreoIndividualProperties individual, EnvioCorreoProperties envio) {
        return new EnviadorCorreoApi(new CorreoProperties.Api(URL_MASIVA, "secreto", "Authorization", "Bearer", null,
                Duration.ofSeconds(5), Duration.ofSeconds(15), null), cliente, envio, individual);
    }

    private EnviadorCorreoApi enviador(CorreoIndividualProperties individual) {
        return enviador(individual, EnvioCorreoProperties.SIN_AJUSTES);
    }

    private static CorreoIndividualProperties individual(String url, String metodo) {
        return new CorreoIndividualProperties(url, metodo, "destinatario");
    }

    private void reenviar(EnviadorCorreoApi enviador) {
        enviador.reenviar(DESTINO, "Asunto", "Texto", "reenvio-7-1", Map.of("Mes", "octubre 2026"));
    }

    @Test
    void elReenvioLlamaALaApiIndividualConLaDireccionDeLaPersonaEnElParametro() {
        servidor.expect(requestTo(URL_INDIVIDUAL + "?destinatario=fsepa@coopsanjose.fin.ec"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secreto"))
                .andExpect(content().string(""))
                .andRespond(withSuccess());

        reenviar(enviador(individual(URL_INDIVIDUAL, "POST")));

        servidor.verify();
    }

    @Test
    void siElContratoDeLaApiEsGetSeUsaGet() {
        servidor.expect(requestTo(URL_INDIVIDUAL + "?destinatario=fsepa@coopsanjose.fin.ec"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        reenviar(enviador(individual(URL_INDIVIDUAL, "get")));

        servidor.verify();
    }

    @Test
    void elParametroPuedeTenerOtroNombre() {
        servidor.expect(requestTo(URL_INDIVIDUAL + "?to=fsepa@coopsanjose.fin.ec")).andRespond(withSuccess());

        reenviar(enviador(new CorreoIndividualProperties(URL_INDIVIDUAL, "POST", "to")));

        servidor.verify();
    }

    @Test
    void conRedireccionElParametroLlevaLaDireccionDePruebasYNuncaLaReal() {
        servidor.expect(requestTo(URL_INDIVIDUAL + "?destinatario=prueba@coopsanjose.fin.ec"))
                .andRespond(withSuccess());

        reenviar(enviador(individual(URL_INDIVIDUAL, "POST"),
                new EnvioCorreoProperties(null, null, null, "prueba@coopsanjose.fin.ec")));

        servidor.verify();
    }

    @Test
    void sinUrlIndividualElReenvioUsaElEnvioMasivoConUnSoloAviso() {
        servidor.expect(requestTo(URL_MASIVA))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.notifications", hasSize(1)))
                .andExpect(jsonPath("$.notifications[0].destinatario").value(DESTINO))
                .andRespond(withSuccess());

        reenviar(enviador(CorreoIndividualProperties.SIN_CONFIGURAR));

        servidor.verify();
    }

    @Test
    void unMetodoQueLaApiNoAceptaExplicaQueHayQueCambiar() {
        servidor.expect(requestTo(URL_INDIVIDUAL + "?destinatario=fsepa@coopsanjose.fin.ec"))
                .andRespond(withStatus(HttpStatus.METHOD_NOT_ALLOWED));

        assertThatThrownBy(() -> reenviar(enviador(individual(URL_INDIVIDUAL, "POST"))))
                .isInstanceOfSatisfying(EnvioCorreoException.class, e -> {
                    assertThat(e.getMessage()).contains("405").contains("POST").contains("GET");
                    assertThat(e.esReintentable()).isFalse();
                });
    }

    @Test
    void unMetodoInvalidoSeRechazaSinLlamarALaApi() {
        assertThatThrownBy(() -> reenviar(enviador(individual(URL_INDIVIDUAL, "DELETE"))))
                .isInstanceOfSatisfying(EnvioCorreoException.class,
                        e -> assertThat(e.getMessage()).contains("GET o POST"));
        servidor.verify();
    }

    @Test
    void unaUrlIndividualMalEscritaSeExplicaSinRepetirlaYNoLlamaALaApi() {
        assertThatThrownBy(() -> reenviar(enviador(individual("{{SERVIDOR}}/send-prueba", "POST"))))
                .isInstanceOfSatisfying(EnvioCorreoException.class,
                        e -> assertThat(e.getMessage()).contains("llaves").doesNotContain("SERVIDOR"));
        servidor.verify();
    }

    @Test
    void siLaApiDiceQueNoAceptoElEnvioSeInforma() {
        servidor.expect(requestTo(URL_INDIVIDUAL + "?destinatario=fsepa@coopsanjose.fin.ec"))
                .andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> reenviar(enviador(individual(URL_INDIVIDUAL, "POST"))))
                .isInstanceOfSatisfying(EnvioCorreoException.class,
                        e -> assertThat(e.getMessage()).contains("no aceptó"));
    }

    @Test
    void unaRespuestaDeErrorDelServidorSeClasificaComoSiempre() {
        servidor.expect(requestTo(URL_INDIVIDUAL + "?destinatario=fsepa@coopsanjose.fin.ec"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> reenviar(enviador(individual(URL_INDIVIDUAL, "POST"))))
                .isInstanceOfSatisfying(EnvioCorreoException.class,
                        e -> assertThat(e.getMessage()).contains("credenciales"));
    }

    @Test
    void elEnviadorSimuladoReenviaConElMismoEnvioDeSiempre() {
        assertThatCode(() -> new EnviadorCorreoSimulado(CorreoProperties.ResultadoSimulado.OK)
                .reenviar(DESTINO, "Asunto", "Texto", "reenvio-7-1", Map.of())).doesNotThrowAnyException();
        assertThatThrownBy(() -> new EnviadorCorreoSimulado(CorreoProperties.ResultadoSimulado.FALLO_DEFINITIVO)
                .reenviar(DESTINO, "Asunto", "Texto", "reenvio-7-1", Map.of()))
                .isInstanceOfSatisfying(EnvioCorreoException.class,
                        e -> assertThat(e.getMessage()).contains("Fallo simulado"));
    }
}
