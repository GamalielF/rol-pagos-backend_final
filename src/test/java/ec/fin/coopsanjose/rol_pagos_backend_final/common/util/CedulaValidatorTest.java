package ec.fin.coopsanjose.rol_pagos_backend_final.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CedulaValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"1710034065", "1712345675", "1709876542", "1723456784"})
    void aceptaCedulasValidas(String cedula) {
        assertThat(CedulaValidator.esValida(cedula)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1710034066", "1760034065", "0000000000", "2510034065", "171003406", "17100340651", "17A0034065"})
    void rechazaCedulasInvalidas(String cedula) {
        assertThat(CedulaValidator.esValida(cedula)).isFalse();
    }
}
