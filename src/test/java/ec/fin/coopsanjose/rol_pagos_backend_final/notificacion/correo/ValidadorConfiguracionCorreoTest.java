package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorConfiguracionCorreoTest {

    @Test
    void unaUrlCorrectaNoTieneProblemas() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("https://correo.interno.test/api/enviar")).isNull();
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("http://localhost:8080/api/enviar")).isNull();
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("  HTTPS://correo.test/x  ")).isNull();
    }

    @Test
    void unaUrlVaciaOEnBlancoSeDiceAsi() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl(null)).contains("vacía");
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("   ")).contains("vacía");
    }

    @Test
    void lasComillasDelEnvSeSeñalanConLaSolucion() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("\"https://correo.test/api\"")).contains("comillas").contains("sin comillas");
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("'https://correo.test/api'")).contains("comillas");
    }

    @Test
    void unaPlantillaDePostmanSeReconoce() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("{{PROTOCOLO}}{{SERVER_NAME}}/notificaciones/send"))
                .contains("llaves").contains("Postman");
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("https://{{SERVER_NAME}}/api")).contains("llaves");
    }

    @Test
    void losEspaciosDentroDeLaUrlSeSeñalan() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("https://correo.test/api enviar")).contains("espacios");
    }

    @Test
    void sinHttpsAlPrincipioSeDice() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("correo.test/api")).contains("https://");
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("ftp://correo.test/api")).contains("https://");
    }

    @Test
    void sinServidorDespuesDelEsquemaSeDice() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("https://")).isNotNull();
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl("https:///solo/ruta")).isNotNull();
    }

    @Test
    void elMensajeNuncaRepiteLaUrlRevisada() {
        String conClave = "https://correo.test/api?clave=SECRETO123 espacio";
        assertThat(ValidadorConfiguracionCorreo.problemaDeUrl(conClave)).isNotNull().doesNotContain("SECRETO123").doesNotContain("correo.test");
    }

    @Test
    void unTokenNormalOAusenteEstaBien() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeToken(null)).isNull();
        assertThat(ValidadorConfiguracionCorreo.problemaDeToken("")).isNull();
        assertThat(ValidadorConfiguracionCorreo.problemaDeToken("abc.DEF-123_xyz")).isNull();
    }

    @Test
    void unTokenConSaltosDeLineaOSimbolosRarosSeSeñalaSinRepetirlo() {
        assertThat(ValidadorConfiguracionCorreo.problemaDeToken("abc\n")).contains("caracteres no permitidos");
        assertThat(ValidadorConfiguracionCorreo.problemaDeToken("abc\r")).contains("caracteres no permitidos");
        assertThat(ValidadorConfiguracionCorreo.problemaDeToken("tokén€")).contains("caracteres no permitidos").doesNotContain("tokén");
    }
}
