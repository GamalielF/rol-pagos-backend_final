package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Regla del proyecto: ninguna clave, token o secreto va escrito en los archivos de configuración. */
class SinSecretosEnConfiguracionTest {

    private static final Pattern LINEA = Pattern.compile("^\\s*([A-Za-z0-9_.\\-\\[\\]']+)\\s*[:=]\\s*(.*?)\\s*$");
    private static final Pattern CLAVE_SENSIBLE =
            Pattern.compile("(?i)(password|passwd|secret|token|api[-_.]?key|private[-_.]?key)");

    @Test
    void ningunSecretoVaEscritoEnLosArchivosDeConfiguracion() throws IOException {
        List<String> violaciones = new ArrayList<>();
        try (Stream<Path> archivos = Files.list(Path.of("src/main/resources"))) {
            for (Path archivo : archivos.filter(this::esConfiguracion).toList()) {
                for (String linea : Files.readAllLines(archivo)) {
                    if (linea.isBlank() || linea.trim().startsWith("#")) {
                        continue;
                    }
                    Matcher m = LINEA.matcher(linea);
                    if (!m.matches() || !CLAVE_SENSIBLE.matcher(m.group(1)).find()) {
                        continue;
                    }
                    String valor = m.group(2).replaceAll("\\s+#.*$", "").replaceAll("^['\"]|['\"]$", "");
                    if (!valor.isEmpty() && !valor.startsWith("${")) {
                        violaciones.add(archivo.getFileName() + " -> " + m.group(1) + " tiene un valor escrito");
                    }
                }
            }
        }
        assertThat(violaciones).as("Claves escritas en archivos de configuración").isEmpty();
    }

    private boolean esConfiguracion(Path archivo) {
        String nombre = archivo.getFileName().toString();
        return nombre.startsWith("application")
                && (nombre.endsWith(".yml") || nombre.endsWith(".yaml") || nombre.endsWith(".properties"));
    }
}
