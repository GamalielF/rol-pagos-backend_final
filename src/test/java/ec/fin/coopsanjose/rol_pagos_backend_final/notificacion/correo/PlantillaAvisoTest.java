package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaAvisoTest {

    private static final YearMonth AGOSTO = YearMonth.of(2026, 8);

    @Test
    void elAsuntoNombraElMesEnEspanol() {
        assertThat(PlantillaAviso.asunto(AGOSTO)).isEqualTo("Su rol de pago de agosto de 2026 está disponible");
        assertThat(PlantillaAviso.asunto(YearMonth.of(2027, 1))).contains("enero de 2027");
    }

    @Test
    void elCuerpoIncluyeElEnlaceSoloSiExiste() {
        assertThat(PlantillaAviso.cuerpo(AGOSTO, "https://roles.ejemplo.test")).contains("https://roles.ejemplo.test");
        assertThat(PlantillaAviso.cuerpo(AGOSTO, "")).doesNotContain("http").doesNotContain("\n\n\n");
        assertThat(PlantillaAviso.cuerpo(AGOSTO, null)).doesNotContain("http");
    }

    @Test
    void elCuerpoNoTraeDatosDelRolDePago() {
        String cuerpo = PlantillaAviso.cuerpo(AGOSTO, "https://roles.ejemplo.test");

        assertThat(cuerpo).contains("no contiene información de su rol de pago")
                .doesNotContain("$")
                .doesNotContainIgnoringCase("sueldo")
                .doesNotContainIgnoringCase("salario")
                .doesNotContainIgnoringCase("neto");
    }

    @Test
    void elCuerpoAdviertequeNuncaSePideLaClave() {
        assertThat(PlantillaAviso.cuerpo(AGOSTO, null)).contains("nunca le solicitaremos su clave");
    }
}
