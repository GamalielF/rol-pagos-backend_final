package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Fabrica PDFs sintéticos para las pruebas: nada de datos reales. */
public final class PdfDePrueba {

    private PdfDePrueba() {
    }

    public static byte[] crear(String... lineas) {
        return crear(1, lineas);
    }

    public static byte[] crear(int paginas, String... lineas) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) {
                PDPage pagina = new PDPage();
                documento.addPage(pagina);
                try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                    contenido.beginText();
                    contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contenido.setLeading(16f);
                    contenido.newLineAtOffset(50, 700);
                    for (String linea : lineas) {
                        contenido.showText(linea);
                        contenido.newLine();
                    }
                    contenido.endText();
                }
            }
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reproduce el defecto de los reportes reales: bytes en blanco antes de "%PDF-". */
    public static byte[] conBasuraAlInicio(byte[] pdf) {
        byte[] basura = "\n\n    \n\n    ".getBytes(StandardCharsets.US_ASCII);
        byte[] resultado = new byte[basura.length + pdf.length];
        System.arraycopy(basura, 0, resultado, 0, basura.length);
        System.arraycopy(pdf, 0, resultado, basura.length, pdf.length);
        return resultado;
    }
}
