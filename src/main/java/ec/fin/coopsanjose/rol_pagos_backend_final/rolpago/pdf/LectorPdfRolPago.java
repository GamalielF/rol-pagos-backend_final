package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lee los datos que necesitamos validar de un rol de pago. No toca la base de datos. */
@Component
public class LectorPdfRolPago {

    /** Datos del PDF. texto va en MAYÚSCULAS y sin tildes. */
    public record DatosPdf(int paginas, Optional<YearMonth> periodo, String texto) {
    }

    private static final byte[] CABECERA = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int VENTANA_CABECERA = 1024;
    private static final List<String> MESES = List.of("ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE");
    private static final Pattern MES_ANIO = Pattern.compile(
            "\\b(ENERO|FEBRERO|MARZO|ABRIL|MAYO|JUNIO|JULIO|AGOSTO|SEPTIEMBRE|OCTUBRE|NOVIEMBRE|DICIEMBRE)\\s+(\\d{4})\\b");

    /**
     * Descarta los bytes en blanco que algunos sistemas escriben antes de "%PDF-" (los reportes reales traen 12).
     * Devuelve vacío si no es un PDF.
     */
    public Optional<byte[]> normalizar(byte[] bytes) {
        if (bytes == null) {
            return Optional.empty();
        }
        int limite = Math.min(bytes.length, VENTANA_CABECERA) - CABECERA.length;
        for (int i = 0; i <= limite; i++) {
            if (Arrays.equals(bytes, i, i + CABECERA.length, CABECERA, 0, CABECERA.length)) {
                return Optional.of(i == 0 ? bytes : Arrays.copyOfRange(bytes, i, bytes.length));
            }
        }
        return Optional.empty();
    }

    /** @throws IOException si el PDF está dañado, es ilegible o está protegido con clave */
    public DatosPdf leer(byte[] pdfNormalizado) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdfNormalizado)) {
            PDFTextStripper extractor = new PDFTextStripper();
            extractor.setStartPage(1);
            extractor.setEndPage(1);
            String texto = normalizarTexto(extractor.getText(documento));
            return new DatosPdf(documento.getNumberOfPages(), buscarPeriodo(texto), texto);
        }
    }

    /**
     * ¿Aparecen TODAS las palabras del nombre en el PDF? Sin importar el orden ni las tildes:
     * "Nuñez Albiño Anita" coincide con "ANITA NUNEZ ALBINO". Palabras completas, no fragmentos.
     */
    public boolean contieneNombre(DatosPdf datos, String nombre) {
        String normalizado = normalizarTexto(nombre);
        if (normalizado.isEmpty()) {
            return false;
        }
        for (String palabra : normalizado.split(" ")) {
            Pattern completa = Pattern.compile("\\b" + Pattern.quote(palabra) + "\\b");
            if (!completa.matcher(datos.texto()).find()) {
                return false;
            }
        }
        return true;
    }

    private Optional<YearMonth> buscarPeriodo(String texto) {
        Matcher m = MES_ANIO.matcher(texto);
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(YearMonth.of(Integer.parseInt(m.group(2)), MESES.indexOf(m.group(1)) + 1));
    }

    static String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}