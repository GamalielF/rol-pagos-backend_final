package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.CoincidenciaNombre.Diferencia;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.CoincidenciaNombre.Resultado;
import org.junit.jupiter.api.Test;

class CoincidenciaNombreTest {

    /** El texto de un rol real (ya normalizado): el nombre va junto a otras palabras del recibo. */
    private static final String RECIBO_SANDRA = "COOPERATIVA DE AHORRO Y CREDITO SAN JOSE LTDA AGENCIA VENTANAS MES ANO SEPTIEMBRE 2026 "
            + "NOMBRE CARGO CURI LLUMITAXI SANDRA GUALADUPE ASESOR DE INVERSIONES INGRESOS EGRESOS SUELDO 1100,00 "
            + "REMUNERACION APORTE GENESIS TELEFONIA CLARO DESCUENTO SUELDO";

    // ------------------------------------------------------------ coincidencia exacta (la regla de siempre)

    @Test
    void laCoincidenciaExactaNoImportaElOrden() {
        Resultado r = CoincidenciaNombre.evaluar("NOMBRE NUNEZ ALBINO ANITA MARCELA CARGO CAJERA", "ANITA MARCELA NUNEZ ALBINO");
        assertThat(r.coincide()).isTrue();
        assertThat(r.exacta()).isTrue();
        assertThat(r.diferencias()).isEmpty();
    }

    @Test
    void unaPalabraConPuntuacionCuentaComoExactaSiApareceCompleta() {
        Resultado r = CoincidenciaNombre.evaluar("NOMBRE O'BRIEN PEREZ MARIA ELENA CARGO", "O'BRIEN PEREZ MARIA ELENA");
        assertThat(r.exacta()).isTrue();
    }

    // ------------------------------------------------------------ una errata de digitación se tolera y se informa

    @Test
    void elCasoRealDeSandraSeAceptaYSeInformaLaErrata() {
        Resultado r = CoincidenciaNombre.evaluar(RECIBO_SANDRA, "CURI LLUMITAXI SANDRA GUADALUPE");
        assertThat(r.coincide()).isTrue();
        assertThat(r.exacta()).isFalse();
        assertThat(r.diferencias()).hasSize(1);
        Diferencia d = r.diferencias().get(0);
        assertThat(d.enElAd()).isEqualTo("GUADALUPE");
        assertThat(d.enElPdf()).isEqualTo("GUALADUPE");
    }

    @Test
    void laErrataSeToleraAunqueElNombreDelAdVengaEnOtroOrden() {
        Resultado r = CoincidenciaNombre.evaluar(RECIBO_SANDRA, "GUADALUPE SANDRA CURI LLUMITAXI");
        assertThat(r.coincide()).isTrue();
        assertThat(r.diferencias()).hasSize(1);
    }

    @Test
    void ochoLetrasToleranUnaLetraDeDiferencia() {
        Resultado r = CoincidenciaNombre.evaluar("NOMBRE GAROFLAO RONQUILLO JHON ADRIAN CARGO CAJERO", "GAROFALO RONQUILLO JHON ADRIAN");
        assertThat(r.coincide()).isTrue();
        assertThat(r.diferencias().get(0).enElPdf()).isEqualTo("GAROFLAO");
    }

    // ------------------------------------------------------------ lo que NO se debe aceptar

    @Test
    void otraPersonaNoCoincide() {
        Resultado r = CoincidenciaNombre.evaluar("NOMBRE NUNEZ ALBINO ANITA MARCELA CARGO", "GOMEZ RUIZ MARIA ELENA");
        assertThat(r.coincide()).isFalse();
        assertThat(r.diferencias()).isEmpty();
    }

    @Test
    void unaPersonaConLosMismosApellidosPeroOtroNombreNoCoincide() {
        Resultado r = CoincidenciaNombre.evaluar(RECIBO_SANDRA, "CURI LLUMITAXI SANDRA PATRICIA");
        assertThat(r.coincide()).isFalse();
    }

    @Test
    void dosPalabrasDistintasNoSeToleran() {
        Resultado r = CoincidenciaNombre.evaluar("NOMBRE CURI LLUMITAXI SANDRA GUALADUPE CARGO", "CURI LLUMITAXY SANDRA GUADALUPE");
        assertThat(r.coincide()).isFalse();
    }

    @Test
    void lasPalabrasCortasSiempreSonExactas() {
        assertThat(CoincidenciaNombre.evaluar("NOMBRE PEREZ LOPEZ ANITO CARGO", "PEREZ LOPEZ ANITA").coincide()).isFalse();
        assertThat(CoincidenciaNombre.evaluar("NOMBRE PEREZ LOPEZ ANDREA CARGO", "PEREZ LOPEZ ANDRES").coincide()).isFalse();
    }

    @Test
    void sieteLetrasTampocoSeToleran() {
        assertThat(CoincidenciaNombre.evaluar("NOMBRE RUIZ VERA MARCELO CARGO", "RUIZ VERA MARCELA").coincide()).isFalse();
    }

    @Test
    void masDeLaDiferenciaPermitidaNoSeAcepta() {
        assertThat(CoincidenciaNombre.evaluar("NOMBRE CURI LLUMITAXI SANDRA GUALEDUPO CARGO", "CURI LLUMITAXI SANDRA GUADALUPE").coincide()).isFalse();
        assertThat(CoincidenciaNombre.evaluar("NOMBRE GAROFLXO RONQUILLO JHON ADRIAN CARGO", "GAROFALO RONQUILLO JHON ADRIAN").coincide()).isFalse();
    }

    @Test
    void unNombreDeDosPalabrasNoTieneBaseParaTolerar() {
        assertThat(CoincidenciaNombre.evaluar("NOMBRE RONQUILLO GAROFALOS CARGO", "RONQUILLO GAROFALO").coincide()).isFalse();
    }

    @Test
    void unaPalabraParecidaPeroLejosDelNombreNoCuenta() {
        String texto = "NOMBRE CURI LLUMITAXI SANDRA CARGO ASESOR DE INVERSIONES INGRESOS EGRESOS SUELDO HORAS EXTRAS GUALADUPE";
        assertThat(CoincidenciaNombre.evaluar(texto, "CURI LLUMITAXI SANDRA GUADALUPE").coincide()).isFalse();
    }

    @Test
    void siHayDosCandidatasIgualDeCercanasSeRechazaPorAmbigua() {
        String texto = "NOMBRE CURI LLUMITAXI SANDRA GUADALUPA GUADALUPT CARGO";
        assertThat(CoincidenciaNombre.evaluar(texto, "CURI LLUMITAXI SANDRA GUADALUPE").coincide()).isFalse();
    }

    @Test
    void unNombreVacioONuloNuncaCoincide() {
        assertThat(CoincidenciaNombre.evaluar(RECIBO_SANDRA, "").coincide()).isFalse();
        assertThat(CoincidenciaNombre.evaluar(RECIBO_SANDRA, "   ").coincide()).isFalse();
        assertThat(CoincidenciaNombre.evaluar(RECIBO_SANDRA, null).coincide()).isFalse();
        assertThat(CoincidenciaNombre.evaluar(null, "CURI LLUMITAXI SANDRA").coincide()).isFalse();
    }

    // ------------------------------------------------------------ el aviso de qué palabra falta

    @Test
    void cuandoNoCoincideDiceQuePalabrasDelAdNoAparecen() {
        Resultado r = CoincidenciaNombre.evaluar("NOMBRE CURI LLUMITAXI SANDRA GUALEDUPO CARGO", "CURI LLUMITAXI SANDRA GUADALUPE");
        assertThat(r.coincide()).isFalse();
        assertThat(r.noEncontradas()).hasSize(1);
        assertThat(r.noEncontradas().get(0)).isEqualTo("GUADALUPE");
    }

    // ------------------------------------------------------------ la distancia

    @Test
    void laDistanciaCuentaLasLetrasDistintas() {
        assertThat(CoincidenciaNombre.distancia("GUADALUPE", "GUALADUPE")).isEqualTo(2);
        assertThat(CoincidenciaNombre.distancia("GUADALUPE", "GUADALUPE")).isEqualTo(0);
        assertThat(CoincidenciaNombre.distancia("CRISTIAN", "CRISTINA")).isEqualTo(1);
        assertThat(CoincidenciaNombre.distancia("PATRICIA", "PATRICIO")).isEqualTo(1);
        assertThat(CoincidenciaNombre.distancia("", "ANA")).isEqualTo(3);
    }
}
