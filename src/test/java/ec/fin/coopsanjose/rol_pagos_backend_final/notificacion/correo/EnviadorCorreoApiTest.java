package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * El adaptador de la API es un esqueleto: aquí se prueba lo que NO depende del contrato (autenticación,
 * idempotencia, clasificación de errores, privacidad de los mensajes). El cuerpo del mensaje se probará
 * cuando se conozca el contrato real.
 */
class EnviadorCorreoApiTest {

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

    private EnviadorCorreoApi enviador(String url, String token, String esquema, String idempotencia) {
        return new EnviadorCorreoApi(new CorreoProperties.Api(url, token, "Authorization", esquema, idempotencia,
                Duration.ofSeconds(5), Duration.ofSeconds(15), null), cliente);
    }

    private void enviar(EnviadorCorreoApi enviador) {
        enviador.enviar(DESTINO, "Asunto", "Texto", "aviso-7");
    }

    @Test
    void sinUrlNoEstaConfiguradoYNoLlamaALaApi() {
        EnviadorCorreoApi sinUrl = enviador("  ", "secreto", "Bearer", null);

        assertThat(sinUrl.estaConfigurado()).isFalse();
        assertThatThrownBy(() -> enviar(sinUrl)).isInstanceOfSatisfying(EnvioCorreoException.class,
                e -> assertThat(e.esReintentable()).isTrue());
        servidor.verify();   // no se esperaba ninguna llamada
    }

    @Test
    void unEnvioExitosoLlevaLaAutenticacionYLaClaveDeIdempotencia() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secreto"))
                .andExpect(header("Idempotency-Key", "aviso-7"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess());

        assertThatCode(() -> enviar(enviador(URL, "secreto", "Bearer", "Idempotency-Key"))).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    void sinEsquemaElTokenViajaSolo() {
        servidor.expect(requestTo(URL)).andExpect(header("Authorization", "secreto")).andRespond(withSuccess());

        enviar(enviador(URL, "secreto", "", null));
        servidor.verify();
    }

    @Test
    void sinTokenNoSeEnviaCabeceraDeAutorizacion() {
        servidor.expect(requestTo(URL)).andExpect(headerDoesNotExist("Authorization")).andRespond(withSuccess());

        enviar(enviador(URL, "", "Bearer", null));
        servidor.verify();
    }

    @Test
    void sinClaveDeIdempotenciaNoSeEnviaLaCabecera() {
        servidor.expect(requestTo(URL)).andExpect(headerDoesNotExist("Idempotency-Key")).andRespond(withSuccess());

        enviar(enviador(URL, "secreto", "Bearer", ""));
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource({"400,false", "409,false", "422,false",
            "401,true", "403,true", "404,true", "408,true", "429,true", "500,true", "503,true"})
    void cadaCodigoHttpSeClasificaComoDefinitivoOReintentable(int codigo, boolean reintentable) {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(codigo)));

        assertThatThrownBy(() -> enviar(enviador(URL, "secreto", "Bearer", null)))
                .isInstanceOfSatisfying(EnvioCorreoException.class,
                        e -> assertThat(e.esReintentable()).isEqualTo(reintentable));
    }

    private void fallaConRed(IOException error, String mensajeEsperado) {
        servidor.expect(requestTo(URL)).andRespond(withException(error));

        assertThatThrownBy(() -> enviar(enviador(URL, "secreto", "Bearer", null)))
                .isInstanceOfSatisfying(EnvioCorreoException.class, e -> {
                    assertThat(e.esReintentable()).isTrue();
                    assertThat(e.getMessage()).contains(mensajeEsperado);
                });
    }

    @Test
    void unTiempoDeEsperaAgotadoSeReintenta() {
        fallaConRed(new SocketTimeoutException("lectura"), "no respondió a tiempo");
    }

    @Test
    void unaConexionRechazadaSeReintenta() {
        fallaConRed(new ConnectException("rechazada"), "No se pudo conectar");
    }

    @Test
    void unFalloDeCertificadoSeReintentaConSuPropioMensaje() {
        fallaConRed(new SSLHandshakeException("certificado"), "conexión segura");
    }

    @Test
    void losMensajesDeErrorNuncaFiltranLaDireccionNiElToken() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(500)));
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException(DESTINO + " secreto")));
        EnviadorCorreoApi api = enviador(URL, "secreto", "Bearer", null);

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> enviar(api)).isInstanceOfSatisfying(EnvioCorreoException.class, e ->
                    assertThat(e.getMessage()).doesNotContain(DESTINO).doesNotContain("secreto").doesNotContain("@"));
        }
    }
}