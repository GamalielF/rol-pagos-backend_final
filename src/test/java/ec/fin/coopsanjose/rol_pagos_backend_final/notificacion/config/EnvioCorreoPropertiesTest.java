package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EnvioCorreoPropertiesTest {

    @Test
    void sinConfiguracionUsaCanalEmailSinPlantillaNiRedireccion() {
        EnvioCorreoProperties p = EnvioCorreoProperties.desde(new MockEnvironment());
        assertThat(p.canal()).isEqualTo("EMAIL");
        assertThat(p.usaPlantilla()).isFalse();
        assertThat(p.redirige()).isFalse();
        assertThat(p.variables()).isEmpty();
    }

    @Test
    void leeLosValoresDeAppCorreoApi() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("app.correo.api.codigo-plantilla", "ROL_PAGO_DISPONIBLE")
                .withProperty("app.correo.api.redirigir-a", "prueba@coopsanjose.fin.ec")
                .withProperty("app.correo.api.variables.NombreEmpresa", "COOPSANJOSE LTDA");
        EnvioCorreoProperties p = EnvioCorreoProperties.desde(entorno);
        assertThat(p.usaPlantilla()).isTrue();
        assertThat(p.codigoPlantilla()).isEqualTo("ROL_PAGO_DISPONIBLE");
        assertThat(p.redirige()).isTrue();
        assertThat(p.redirigirA()).isEqualTo("prueba@coopsanjose.fin.ec");
        assertThat(p.variables()).hasSize(1).containsValue("COOPSANJOSE LTDA");
    }

    @Test
    void textosEnBlancoSeTratanComoNoConfigurados() {
        EnvioCorreoProperties p = new EnvioCorreoProperties("  ", "  ", null, "  ");
        assertThat(p.canal()).isEqualTo("EMAIL");
        assertThat(p.usaPlantilla()).isFalse();
        assertThat(p.redirige()).isFalse();
    }

    @Test
    void unEntornoQueNoSePuedeLeerUsaLosValoresPorDefecto() {
        assertThat(EnvioCorreoProperties.desde(mock(Environment.class))).isSameAs(EnvioCorreoProperties.SIN_AJUSTES);
    }
}