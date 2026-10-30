package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class CorreoIndividualPropertiesTest {

    @Test
    void sinNadaConfiguradoElReenvioUsaElEnvioMasivo() {
        CorreoIndividualProperties p = CorreoIndividualProperties.desde(new MockEnvironment());

        assertThat(p.configurada()).isFalse();
        assertThat(p.metodo()).isEqualTo("POST");
        assertThat(p.parametroDestinatario()).isEqualTo("destinatario");
    }

    @Test
    void laUrlLaDelMetodoYElNombreDelParametroSeLeenDelEntorno() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("app.correo.individual.url", "https://correo.interno.test/api/send-prueba")
                .withProperty("app.correo.individual.metodo", "GET")
                .withProperty("app.correo.individual.parametro-destinatario", "to");

        CorreoIndividualProperties p = CorreoIndividualProperties.desde(entorno);

        assertThat(p.configurada()).isTrue();
        assertThat(p.url()).isEqualTo("https://correo.interno.test/api/send-prueba");
        assertThat(p.metodo()).isEqualTo("GET");
        assertThat(p.parametroDestinatario()).isEqualTo("to");
    }

    @Test
    void conSoloLaUrlElMetodoYElParametroTomanSusValoresPorDefecto() {
        CorreoIndividualProperties p = CorreoIndividualProperties.desde(
                new MockEnvironment().withProperty("app.correo.individual.url", "https://correo.interno.test/api/send-prueba"));

        assertThat(p.configurada()).isTrue();
        assertThat(p.metodo()).isEqualTo("POST");
        assertThat(p.parametroDestinatario()).isEqualTo("destinatario");
    }

    @Test
    void unaUrlEnBlancoNoCuentaComoConfigurada() {
        assertThat(CorreoIndividualProperties.desde(new MockEnvironment().withProperty("app.correo.individual.url", "  ")).configurada())
                .isFalse();
    }
}
