package ec.fin.coopsanjose.rol_pagos_backend_final.common.prueba;

import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.LectorPdfRolPago;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class DemoPdfControllerTest {

    @Test
    void elPdfGeneradoPasaLaValidacionDeLaCargaReal(@TempDir Path carpeta) throws Exception {
        Path pdf = carpeta.resolve("1712345675.pdf");
        DemoPdfController.crearPdf(pdf, YearMonth.of(2026, 9), "NUÑEZ ALBIÑO ANITA");

        LectorPdfRolPago lector = new LectorPdfRolPago();
        LectorPdfRolPago.DatosPdf datos = lector.leer(lector.normalizar(Files.readAllBytes(pdf)).orElseThrow());

        assertThat(datos.paginas()).isEqualTo(1);
        assertThat(datos.periodo()).contains(YearMonth.of(2026, 9));
        assertThat(lector.contieneNombre(datos, "Nuñez Albiño Anita")).isTrue();
        assertThat(lector.contieneNombre(datos, "OTRA PERSONA")).isFalse();
    }
}