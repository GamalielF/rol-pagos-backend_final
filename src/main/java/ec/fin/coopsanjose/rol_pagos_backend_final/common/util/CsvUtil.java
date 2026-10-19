package ec.fin.coopsanjose.rol_pagos_backend_final.common.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Escritura segura de CSV. El separador es ";" porque Excel en español lo espera así. */
public final class CsvUtil {

    public static final String SEPARADOR = ";";
    private static final String INICIOS_PELIGROSOS = "=+-@\t\r";

    private CsvUtil() {
    }

    public static String celda(String valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor;
        // Un texto que empiece con = + - @ se interpretaría como fórmula al abrirlo en Excel (inyección CSV)
        if (!texto.isEmpty() && INICIOS_PELIGROSOS.indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        if (texto.contains(SEPARADOR) || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    public static String fila(String... celdas) {
        return Arrays.stream(celdas).map(CsvUtil::celda).collect(Collectors.joining(SEPARADOR)) + "\r\n";
    }
}
