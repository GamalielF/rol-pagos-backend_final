package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdministradoresPropertiesTest {

    @Test
    void normalizaMayusculasEspaciosYRepetidos() {
        AdministradoresProperties p = new AdministradoresProperties(List.of(" FSepa ", "msavala", "fsepa", "  ", ""));

        assertThat(p.administradoresIniciales()).containsExactly("fsepa", "msavala");
    }

    @Test
    void ignoraLosNulosDeLaLista() {
        AdministradoresProperties p = new AdministradoresProperties(Arrays.asList("fsepa", null));

        assertThat(p.administradoresIniciales()).containsExactly("fsepa");
    }

    @Test
    void sinListaNoHayAdministradoresDeArranque() {
        AdministradoresProperties p = new AdministradoresProperties(null);

        assertThat(p.administradoresIniciales()).isEmpty();
        assertThat(p.esAdministradorInicial("fsepa")).isFalse();
    }

    @Test
    void elControlIgnoraMayusculasYEspacios() {
        AdministradoresProperties p = new AdministradoresProperties(List.of("fsepa", "msavala"));

        assertThat(p.esAdministradorInicial("FSEPA")).isTrue();
        assertThat(p.esAdministradorInicial("  msavala ")).isTrue();
        assertThat(p.esAdministradorInicial("otro")).isFalse();
        assertThat(p.esAdministradorInicial(null)).isFalse();
    }

    @Test
    void laListaNoSePuedeAlterarDespuesDeCargada() {
        AdministradoresProperties p = new AdministradoresProperties(List.of("fsepa"));

        assertThatThrownBy(() -> p.administradoresIniciales().add("intruso"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}