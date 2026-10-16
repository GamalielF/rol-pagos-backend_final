package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.storage;

import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.config.RolPagoProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenamientoDiscoTest {

    @TempDir
    Path carpeta;

    private AlmacenamientoDisco almacen() throws IOException {
        return new AlmacenamientoDisco(new RolPagoProperties(carpeta.toString(), DataSize.ofMegabytes(5), 500, 1));
    }

    @Test
    void guardaYLeeElMismoContenido() throws IOException {
        AlmacenamientoDisco almacen = almacen();
        byte[] contenido = "contenido de prueba".getBytes();

        String ruta = almacen.guardar(YearMonth.of(2026, 9), contenido);

        assertThat(ruta).matches("2026-09/[0-9a-f\\-]{36}\\.pdf");
        assertThat(almacen.existe(ruta)).isTrue();
        assertThat(almacen.leer(ruta)).isEqualTo(contenido);
    }

    @Test
    void cadaArchivoRecibeUnNombreDistinto() throws IOException {
        AlmacenamientoDisco almacen = almacen();

        String a = almacen.guardar(YearMonth.of(2026, 9), new byte[]{1});
        String b = almacen.guardar(YearMonth.of(2026, 9), new byte[]{1});

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void reemplazarCambiaElContenidoSinCambiarLaRuta() throws IOException {
        AlmacenamientoDisco almacen = almacen();
        String ruta = almacen.guardar(YearMonth.of(2026, 9), "uno".getBytes());

        almacen.reemplazar(ruta, "dos".getBytes());

        assertThat(almacen.leer(ruta)).isEqualTo("dos".getBytes());
    }

    @Test
    void nuncaSaleDeLaCarpetaBase() throws IOException {
        AlmacenamientoDisco almacen = almacen();

        assertThatThrownBy(() -> almacen.leer("../fuera.pdf")).isInstanceOf(IllegalStateException.class);
        //assertThatThrownBy(() -> almacen.guardar(YearMonth.of(2026, 9), new byte[]{1}).length());   // control: esto sí es válido
        assertThatThrownBy(() -> almacen.reemplazar("../../fuera.pdf", new byte[]{1}))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unArchivoInexistenteNoExisteYAlLeerloFalla() throws IOException {
        AlmacenamientoDisco almacen = almacen();

        assertThat(almacen.existe("2026-09/no-existe.pdf")).isFalse();
        assertThatThrownBy(() -> almacen.leer("2026-09/no-existe.pdf")).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void noDejaArchivosTemporalesDespuesDeGuardar() throws IOException {
        AlmacenamientoDisco almacen = almacen();
        almacen.guardar(YearMonth.of(2026, 9), new byte[]{1, 2, 3});

        try (Stream<Path> todo = Files.walk(carpeta)) {
            List<Path> temporales = todo.filter(p -> p.toString().endsWith(".tmp")).toList();
            assertThat(temporales).isEmpty();
        }
    }
}
