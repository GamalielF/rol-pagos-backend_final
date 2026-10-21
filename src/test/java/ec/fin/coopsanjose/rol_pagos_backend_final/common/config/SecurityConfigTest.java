package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    @ParameterizedTest(name = "devAuth={0}, prod={1}, jwt={2}: arranca")
    @CsvSource({"true,false,false", "true,false,true", "false,false,true", "false,true,true"})
    void lasCombinacionesValidasArrancan(boolean devAuth, boolean prod, boolean conJwt) {
        assertThatCode(() -> SecurityConfig.verificarAutenticacion(devAuth, prod, conJwt)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "devAuth={0}, prod={1}, jwt={2}: se niega a arrancar")
    @CsvSource({"true,true,true,prod", "true,true,false,prod", "false,false,false,No hay forma", "false,true,false,No hay forma"})
    void lasCombinacionesPeligrosasNoArrancan(boolean devAuth, boolean prod, boolean conJwt, String mensaje) {
        assertThatThrownBy(() -> SecurityConfig.verificarAutenticacion(devAuth, prod, conJwt))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(mensaje);
    }
}