package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaAvisoVariablesTest {

    @Test
    void traeLosNombresDeLaPlantillaPorDefectoYLosPropios() {
        Map<String, String> v = PlantillaAviso.variables(YearMonth.of(2026, 9), " http://localhost:4200 ",
                "jperez@coopsanjose.fin.ec", 12L, LocalDate.of(2026, 10, 22));
        assertThat(v).containsEntry("FirstName", "Colaborador/a")
                .containsEntry("UserName", "jperez@coopsanjose.fin.ec")
                .containsEntry("CompanyName", "COOPSANJOSE LTDA")
                .containsEntry("NotificationDate", "2026-10-22")
                .containsEntry("generado", "12")
                .containsEntry("UrlSistema", "http://localhost:4200");
        assertThat(v.get("Mes")).startsWith("sept").endsWith("de 2026");
    }

    @Test
    void ningunValorEsNuloAunqueFalteLaUrlOElCorreo() {
        Map<String, String> v = PlantillaAviso.variables(YearMonth.of(2026, 9), null, null, 1L, LocalDate.of(2026, 10, 22));
        assertThat(v.values()).doesNotContainNull();
        assertThat(v).containsEntry("UrlSistema", "").containsEntry("UserName", "");
    }

    @Test
    void noIncluyeNombresDePersonasNiDatosDelRol() {
        Map<String, String> v = PlantillaAviso.variables(YearMonth.of(2026, 9), "x", "a@b.test", 1L, LocalDate.now());
        assertThat(v.keySet()).noneMatch(k -> k.toLowerCase().contains("cedula") || k.toLowerCase().contains("total"));
    }
}
