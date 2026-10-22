package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.EnvioCorreoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Las variables de cada aviso viajan con la plantilla de la API. */
class EnviadorCorreoApiVariablesTest {

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
        return new EnviadorCorreoApi(new CorreoProperties.Api(URL, "", "Authorization", "Bearer", null,
                Duration.ofSeconds(5), Duration.ofSeconds(15), null), cliente, envio);
    }

    @Test
    void lasVariablesDelAvisoViajanConElCodigoDeLaPlantilla() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.notifications[0].codigoPlantilla").value("ACCOUNT_ACTIVATE"))
                .andExpect(jsonPath("$.notifications[0].variables.Mes").value("septiembre de 2026"))
                .andExpect(jsonPath("$.notifications[0].variables.UserName").value(DESTINO))
                .andRespond(withSuccess());
        enviador(new EnvioCorreoProperties(null, "ACCOUNT_ACTIVATE", null, null)).enviar(DESTINO, "A", "T", "aviso-7",
                Map.of("Mes", "septiembre de 2026", "UserName", DESTINO));
        servidor.verify();
    }

    @Test
    void loFijadoEnLaConfiguracionPrevaleceSobreLoDelAviso() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.notifications[0].variables.CompanyName").value("OTRA EMPRESA"))
                .andRespond(withSuccess());
        enviador(new EnvioCorreoProperties(null, "ACCOUNT_ACTIVATE", Map.of("CompanyName", "OTRA EMPRESA"), null))
                .enviar(DESTINO, "A", "T", "aviso-7", Map.of("CompanyName", "COOPSANJOSE LTDA"));
        servidor.verify();
    }

    @Test
    void sinPlantillaLasVariablesNoSeEnvian() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.notifications[0].codigoPlantilla").doesNotExist())
                .andExpect(jsonPath("$.notifications[0].variables").doesNotExist())
                .andRespond(withSuccess());
        enviador(EnvioCorreoProperties.SIN_AJUSTES).enviar(DESTINO, "A", "T", "aviso-7", Map.of("Mes", "septiembre"));
        servidor.verify();
    }

    @Test
    void conRedireccionLaDireccionRealTampocoViajaEnLasVariables() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.notifications[0].variables.UserName").value("prueba@coopsanjose.fin.ec"))
                .andExpect(content().string(not(containsString(DESTINO))))
                .andRespond(withSuccess());
        enviador(new EnvioCorreoProperties(null, "ACCOUNT_ACTIVATE", null, "prueba@coopsanjose.fin.ec"))
                .enviar(DESTINO, "A", "T", "aviso-7", Map.of("UserName", DESTINO));
        servidor.verify();
    }

    @Test
    void laFormaDeCuatroParametrosSigueFuncionando() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess());
        enviador(new EnvioCorreoProperties(null, "ACCOUNT_ACTIVATE", null, null)).enviar(DESTINO, "A", "T", "aviso-7");
        servidor.verify();
    }
}