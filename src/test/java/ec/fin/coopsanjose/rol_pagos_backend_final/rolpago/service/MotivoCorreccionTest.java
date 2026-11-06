package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

class MotivoCorreccionTest {

    @Test
    void unMotivoClaroSeAceptaTalCual() {
        assertThat(MotivoCorreccion.validar("Se subió el rol equivocado de Juan")).isEqualTo("Se subió el rol equivocado de Juan");
    }

    @Test
    void losEspaciosRepetidosYDeLosBordesSeLimpian() {
        assertThat(MotivoCorreccion.validar("   Se   subió\t el  rol   mal \n")).isEqualTo("Se subió el rol mal");
    }

    @Test
    void conDiezCaracteresBasta() {
        assertThat(MotivoCorreccion.validar("1234567890")).hasSize(10);
    }

    @Test
    void conMenosDeDiezSeRechaza() {
        assertThatThrownBy(() -> MotivoCorreccion.validar("123456789"))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("motivo");
    }

    @Test
    void nuloVacioOSoloEspaciosSeRechazan() {
        assertThatThrownBy(() -> MotivoCorreccion.validar(null)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> MotivoCorreccion.validar("")).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> MotivoCorreccion.validar("            ")).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void elRellenoDeEspaciosNoEngañaAlMinimo() {
        assertThatThrownBy(() -> MotivoCorreccion.validar("ab        cd")).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void unMotivoLarguisimoSeRecortaA200() {
        assertThat(MotivoCorreccion.validar("a".repeat(500))).hasSize(MotivoCorreccion.MAXIMO);
    }
}
