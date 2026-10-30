package ec.fin.coopsanjose.rol_pagos_backend_final.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvUtilTest {

    @Test
    void unTextoNormalNoSeAltera() {
        assertThat(CsvUtil.celda("hola mundo")).isEqualTo("hola mundo");
    }

    @Test
    void nuloSeConvierteEnVacio() {
        assertThat(CsvUtil.celda(null)).isEmpty();
    }

    @Test
    void neutralizaLasFormulasDeExcel() {
        assertThat(CsvUtil.celda("=1+1")).isEqualTo("'=1+1");
        assertThat(CsvUtil.celda("+cmd")).isEqualTo("'+cmd");
        assertThat(CsvUtil.celda("-2")).isEqualTo("'-2");
        assertThat(CsvUtil.celda("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(CsvUtil.celda("\tcmd")).startsWith("'");
    }

    @Test
    void encierraEnComillasLoQueTieneElSeparadorOSaltosDeLinea() {
        assertThat(CsvUtil.celda("a;b")).isEqualTo("\"a;b\"");
        assertThat(CsvUtil.celda("linea1\nlinea2")).isEqualTo("\"linea1\nlinea2\"");
    }

    @Test
    void duplicaLasComillas() {
        assertThat(CsvUtil.celda("dijo \"hola\"")).isEqualTo("\"dijo \"\"hola\"\"\"");
    }

    @Test
    void unaFormulaConComillasSeNeutralizaYSeEscapa() {
        assertThat(CsvUtil.celda("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
    }

    @Test
    void unaFilaSeUnePorElSeparadorYTerminaEnSaltoDeLinea() {
        assertThat(CsvUtil.fila("a", "b;c", null)).isEqualTo("a;\"b;c\";\r\n");
    }
}
