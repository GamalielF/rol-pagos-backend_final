package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import static org.assertj.core.api.Assertions.assertThat;

import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.EstadoCarga;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoArchivoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/** Un nombre con UNA errata de digitación en el PDF se acepta y se avisa; lo demás se sigue rechazando. */
class CargaConErrataEnElNombreIT extends SoporteRolesPago {

    private static final String NOMBRE_AD = "CURI LLUMITAXI SANDRA GUADALUPE";

    @BeforeEach
    void titular() {
        persona("scuri.prueba", CEDULA_JUAN, NOMBRE_AD, true);
    }

    @Test
    void unaErrataEnUnaPalabraLargaSeAceptaYSeAvisaDeEllaAlUsuario() {
        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, "CURI LLUMITAXI SANDRA GUALADUPE"));

        assertThat(r.estado()).isEqualTo(EstadoCarga.CARGADO);
        assertThat(r.mensaje()).contains("Revise la ortografía del PDF: dice «GUALADUPE» y el AD «GUADALUPE»");
        assertThat(r.titular()).isEqualTo(NOMBRE_AD);
        assertThat(rolDe(CEDULA_JUAN).getCedula()).isEqualTo(CEDULA_JUAN);
    }

    @Test
    void elNombreExactoNoLlevaNingunAviso() {
        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_AD));

        assertThat(r.estado()).isEqualTo(EstadoCarga.CARGADO);
        assertThat(r.mensaje()).isEqualTo("Cargado correctamente");
    }

    @Test
    void laRevisionPreviaTambienAvisaDeLaErrata() {
        ResultadoArchivoDto r = carga.cargar(PERIODO,
                List.of(archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, "CURI LLUMITAXI SANDRA GUALADUPE"))), true, null)
                .resultados().get(0);

        assertThat(r.estado()).isEqualTo(EstadoCarga.CARGADO);
        assertThat(r.mensaje()).contains("Revise la ortografía del PDF");
    }

    @Test
    void siElNombreNoCoincideElRechazoDiceQuePalabraNoAparece() {
        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, "CURI LLUMITAXI SANDRA GUALEDUPO"));

        assertThat(r.estado()).isEqualTo(EstadoCarga.RECHAZADO);
        assertThat(r.mensaje()).contains("no coincide con el del titular en el AD");
        assertThat(r.mensaje()).contains("(en el PDF no aparece: GUADALUPE)");
    }

    @Test
    void otraPersonaConLosMismosApellidosSigueRechazada() {
        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, "CURI LLUMITAXI SANDRA PATRICIA"));

        assertThat(r.estado()).isEqualTo(EstadoCarga.RECHAZADO);
    }

    @Test
    void dosErratasSeguiranSiendoUnRechazo() {
        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, "CURY LLUMITAXI SANDRA GUALADUPE"));

        assertThat(r.estado()).isEqualTo(EstadoCarga.RECHAZADO);
    }
}
