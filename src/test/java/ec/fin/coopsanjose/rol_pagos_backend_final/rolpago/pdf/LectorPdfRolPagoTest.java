package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LectorPdfRolPagoTest {

    private final LectorPdfRolPago lector = new LectorPdfRolPago();

    private LectorPdfRolPago.DatosPdf leer(byte[] pdf) throws Exception {
        return lector.leer(lector.normalizar(pdf).orElseThrow());
    }

    @Test
    void descartaLosBytesEnBlancoAnterioresALaCabecera() throws Exception {
        byte[] original = PdfDePrueba.crear("AGOSTO 2026");
        byte[] conBasura = PdfDePrueba.conBasuraAlInicio(original);

        byte[] normalizado = lector.normalizar(conBasura).orElseThrow();

        assertThat(normalizado).isEqualTo(original);
        assertThat(leer(conBasura).periodo()).contains(YearMonth.of(2026, 8));
    }

    @Test
    void unArchivoQueNoEsPdfSeRechaza() {
        assertThat(lector.normalizar("esto no es un pdf".getBytes())).isEmpty();
        assertThat(lector.normalizar(new byte[0])).isEmpty();
        assertThat(lector.normalizar(null)).isEmpty();
    }

    @Test
    void unPdfDanadoLanzaIoException() {
        byte[] danado = "%PDF-1.4\nesto esta roto".getBytes();

        assertThatThrownBy(() -> lector.leer(danado)).isInstanceOf(java.io.IOException.class);
    }

    @Test
    void detectaElMesYElAnio() throws Exception {
        var datos = leer(PdfDePrueba.crear("LIQUIDACION DE SUELDO", "MES AÑO", "SEPTIEMBRE 2026"));

        assertThat(datos.periodo()).contains(YearMonth.of(2026, 9));
        assertThat(datos.paginas()).isEqualTo(1);
    }

    @Test
    void sinMesLegibleElPeriodoQuedaVacio() throws Exception {
        assertThat(leer(PdfDePrueba.crear("TEXTO SIN FECHA")).periodo()).isEmpty();
    }

    @Test
    void cuentaLasPaginas() throws Exception {
        assertThat(leer(PdfDePrueba.crear(2, "AGOSTO 2026")).paginas()).isEqualTo(2);
    }

    @Test
    void elNombreCoincideSinImportarElOrdenNiLasTildes() throws Exception {
        var datos = leer(PdfDePrueba.crear("AGOSTO 2026", "NUÑEZ ALBIÑO ANITA MARCELA"));

        assertThat(lector.contieneNombre(datos, "Anita Marcela Nuñez Albiño")).isTrue();
        assertThat(lector.contieneNombre(datos, "NUNEZ ALBINO ANITA MARCELA")).isTrue();
    }

    @Test
    void elNombreDeOtraPersonaNoCoincide() throws Exception {
        var datos = leer(PdfDePrueba.crear("AGOSTO 2026", "PEREZ LOPEZ JUAN CARLOS"));

        assertThat(lector.contieneNombre(datos, "GOMEZ RUIZ MARIA ELENA")).isFalse();
    }

    @Test
    void noAceptaCoincidenciasParcialesDePalabras() throws Exception {
        var datos = leer(PdfDePrueba.crear("AGOSTO 2026", "PEREZ LOPEZ ANITA"));

        assertThat(lector.contieneNombre(datos, "PEREZ LOPEZ ANA")).isFalse();
        assertThat(lector.contieneNombre(datos, "PERE LOPEZ ANITA")).isFalse();
    }

    @Test
    void unNombreVacioNuncaCoincide() throws Exception {
        var datos = leer(PdfDePrueba.crear("AGOSTO 2026"));

        assertThat(lector.contieneNombre(datos, "")).isFalse();
        assertThat(lector.contieneNombre(datos, null)).isFalse();
        assertThat(lector.contieneNombre(datos, "   ")).isFalse();
    }
}
