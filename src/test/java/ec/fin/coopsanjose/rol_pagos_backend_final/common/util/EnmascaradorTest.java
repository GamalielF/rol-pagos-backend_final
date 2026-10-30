package ec.fin.coopsanjose.rol_pagos_backend_final.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnmascaradorTest {

    @Test
    void enmascaraUnaCedulaDeDiezDigitos() {
        assertThat(Enmascarador.cedula("1712345675")).isEqualTo("17******75");
    }

    @Test
    void nuloDevuelveNulo() {
        assertThat(Enmascarador.cedula(null)).isNull();
    }

    @Test
    void loQueNoEsUnaCedulaNoSeEnmascara() {
        assertThat(Enmascarador.cedula("123")).isNull();
        assertThat(Enmascarador.cedula("17123456789")).isNull();
    }
}