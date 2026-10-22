package ec.fin.coopsanjose.rol_pagos_backend_final.common.prueba;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.util.Enmascarador;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** SOLO DESARROLLO: genera PDF de demostración con los nombres del directorio, para probar la carga masiva. */
@Profile("dev")
@RestController
@RequestMapping("/prueba/demo")
@RequiredArgsConstructor
public class DemoPdfController {

    private static final Locale ES = Locale.forLanguageTag("es-EC");
    private static final Path CARPETA_BASE = Path.of("data", "demo-pdfs");

    public record PdfGenerado(String archivo, String persona, String cedulaEnmascarada) {
    }

    public record ResultadoDemo(String carpeta, String periodo, List<PdfGenerado> archivos) {
    }

    private final DirectorioAdRepository directorio;

    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    @PostMapping("/pdfs")
    public ApiResponse<ResultadoDemo> generar(
            @RequestParam @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "El período debe tener el formato yyyy-MM") String periodo,
            @RequestParam(defaultValue = "5") @Min(1) @Max(200) int cantidad,
            @RequestParam(required = false) List<String> usuarios) throws IOException {
        YearMonth ym = YearMonth.parse(periodo);
        Path carpeta = CARPETA_BASE.resolve(periodo);
        Files.createDirectories(carpeta);
        List<PdfGenerado> generados = new ArrayList<>();
        for (DirectorioAd persona : elegir(cantidad, usuarios)) {
            Path destino = carpeta.resolve(persona.getCedula() + ".pdf");
            crearPdf(destino, ym, persona.getNombreCompleto());
            generados.add(new PdfGenerado(destino.getFileName().toString(), persona.getNombreCompleto(),
                    Enmascarador.cedula(persona.getCedula())));
        }
        return ApiResponse.ok("PDF de demostración generados",
                new ResultadoDemo(carpeta.toAbsolutePath().toString(), periodo, generados));
    }

    /** Primero las personas pedidas por usuario; luego, por orden alfabético, hasta completar la cantidad. Una persona = una cédula. */
    private List<DirectorioAd> elegir(int cantidad, List<String> usuarios) {
        Set<String> pedidos = usuarios == null ? Set.of()
                : usuarios.stream().map(u -> u.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<DirectorioAd> candidatos = directorio.findAllByEstadoRegistro(EstadoRegistro.ACTIVO).stream()
                .filter(d -> tieneTexto(d.getCedula()) && tieneTexto(d.getNombreCompleto()))
                .sorted(Comparator.comparing(DirectorioAd::getUsernameAd))
                .toList();
        Map<String, DirectorioAd> porCedula = new LinkedHashMap<>();
        candidatos.stream().filter(d -> pedidos.contains(d.getUsernameAd().toLowerCase(Locale.ROOT)))
                .forEach(d -> porCedula.putIfAbsent(d.getCedula(), d));
        for (DirectorioAd d : candidatos) {
            if (porCedula.size() >= cantidad) {
                break;
            }
            porCedula.putIfAbsent(d.getCedula(), d);
        }
        return List.copyOf(porCedula.values());
    }

    static void crearPdf(Path destino, YearMonth periodo, String persona) throws IOException {
        String mes = periodo.getMonth().getDisplayName(TextStyle.FULL, ES).toUpperCase(ES);
        List<String> lineas = List.of(
                "COOPSANJOSE LTDA",
                "ROL DE PAGOS",
                mes + " " + periodo.getYear(),
                "COLABORADOR: " + sinTildes(persona),
                "TOTAL A RECIBIR: 1.234,56",
                "DOCUMENTO DE DEMOSTRACION - NO ES UN ROL DE PAGO REAL");
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            try (PDPageContentStream flujo = new PDPageContentStream(documento, pagina)) {
                flujo.beginText();
                flujo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                flujo.setLeading(20f);
                flujo.newLineAtOffset(60, 760);
                for (String linea : lineas) {
                    flujo.showText(linea);
                    flujo.newLine();
                }
                flujo.endText();
            }
            documento.save(destino.toFile());
        }
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}