package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ¿El nombre que trae el rol corresponde al titular del AD?
 *
 * <p>Primero se aplica la regla de siempre: TODAS las palabras del nombre del AD aparecen, completas, en el texto del PDF
 * (sin importar el orden). Si falla, se tolera <b>una sola errata de digitación</b>, con estas condiciones, todas a la vez:
 * <ul>
 *   <li>la palabra que difiere tiene al menos {@value #LARGO_MINIMO} letras (en palabras cortas, una letra cambiada ya es otro nombre: ANA/ANI, MARCELA/MARCELO);</li>
 *   <li>la diferencia es de 1 letra (palabras de 8 letras) o de hasta 2 (9 o más); un par de letras vecinas cambiadas cuenta como 1;</li>
 *   <li>las demás palabras del nombre coinciden exactas, y son al menos {@value #PALABRAS_EXACTAS_MINIMAS};</li>
 *   <li>la palabra del PDF aparece junto a otra palabra del nombre (a no más de {@value #VENTANA} palabras): así no se confunde con
 *       un rótulo cualquiera del recibo;</li>
 *   <li>hay una sola candidata: si dos palabras del PDF empatan, se rechaza;</li>
 *   <li>la candidata no es, a su vez, otra palabra del nombre.</li>
 * </ul>
 * La aceptación aproximada se informa en {@link Resultado#diferencias()} para que RRHH pueda revisarla.
 */
public final class CoincidenciaNombre {

    /** Una palabra del AD que en el PDF está escrita distinto. */
    public record Diferencia(String enElAd, String enElPdf) {
    }

    /**
     * @param coincide      si el nombre corresponde al titular
     * @param diferencias   las erratas toleradas (vacío = coincidencia exacta)
     * @param noEncontradas si NO coincide: las palabras del AD que no aparecen en el PDF (ayuda a ver la errata)
     */
    public record Resultado(boolean coincide, List<Diferencia> diferencias, List<String> noEncontradas) {

        public static final Resultado NO = new Resultado(false, List.of(), List.of());

        public boolean exacta() {
            return coincide && diferencias.isEmpty();
        }
    }

    static final int LARGO_MINIMO = 8;
    static final int PALABRAS_EXACTAS_MINIMAS = 2;
    static final int VENTANA = 4;
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^A-Z0-9]+");

    private CoincidenciaNombre() {
    }

    /** Ambos textos ya normalizados: mayúsculas, sin tildes y con espacios simples. */
    public static Resultado evaluar(String textoPdf, String nombreAd) {
        if (textoPdf == null || nombreAd == null || nombreAd.isBlank()) {
            return Resultado.NO;
        }
        List<String> palabrasAd = Arrays.stream(nombreAd.split(" ")).filter(p -> !p.isEmpty()).toList();
        List<String> pendientes = new ArrayList<>();
        for (String palabra : palabrasAd) {
            if (!apareceCompleta(palabra, textoPdf)) {
                pendientes.add(palabra);
            }
        }
        if (pendientes.isEmpty()) {
            return new Resultado(true, List.of(), List.of());
        }
        Resultado tolerado = tolerarUnaErrata(textoPdf, palabrasAd, pendientes);
        return tolerado != null ? tolerado : new Resultado(false, List.of(), List.copyOf(pendientes));
    }

    private static boolean apareceCompleta(String palabra, String texto) {
        return Pattern.compile("\\b" + Pattern.quote(palabra) + "\\b").matcher(texto).find();
    }

    private static Resultado tolerarUnaErrata(String textoPdf, List<String> palabrasAd, List<String> pendientes) {
        int exactas = palabrasAd.size() - pendientes.size();
        String faltante = pendientes.get(0);
        if (pendientes.size() != 1 || exactas < PALABRAS_EXACTAS_MINIMAS || faltante.length() < LARGO_MINIMO) {
            return null;
        }
        List<String> palabrasPdf = Arrays.stream(NO_ALFANUMERICO.split(textoPdf)).filter(p -> !p.isEmpty()).toList();
        Set<String> exactasDelAd = new HashSet<>(palabrasAd);
        exactasDelAd.remove(faltante);

        List<Integer> posicionesExactas = new ArrayList<>();
        for (int i = 0; i < palabrasPdf.size(); i++) {
            if (exactasDelAd.contains(palabrasPdf.get(i))) {
                posicionesExactas.add(i);
            }
        }
        int limite = faltante.length() >= 9 ? 2 : 1;
        String mejor = null;
        int mejorDistancia = limite + 1;
        boolean empate = false;
        for (int i = 0; i < palabrasPdf.size(); i++) {
            String candidata = palabrasPdf.get(i);
            if (Math.abs(candidata.length() - faltante.length()) > limite
                    || exactasDelAd.contains(candidata)
                    || !estaCerca(i, posicionesExactas)) {
                continue;
            }
            int distancia = distancia(faltante, candidata);
            if (distancia > limite) {
                continue;
            }
            if (distancia < mejorDistancia) {
                mejor = candidata;
                mejorDistancia = distancia;
                empate = false;
            } else if (distancia == mejorDistancia && !candidata.equals(mejor)) {
                empate = true;
            }
        }
        if (mejor == null || empate) {
            return null;
        }
        return new Resultado(true, List.of(new Diferencia(faltante, mejor)), List.of());
    }

    private static boolean estaCerca(int posicion, List<Integer> posicionesExactas) {
        for (int otra : posicionesExactas) {
            if (Math.abs(otra - posicion) <= VENTANA) {
                return true;
            }
        }
        return false;
    }

    /** Distancia de edición con intercambio de letras vecinas (OSA): insertar, borrar, cambiar o intercambiar una letra cuenta 1. */
    static int distancia(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + costo);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }
}
