package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PublicacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResumenPeriodoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicacionServiceIT extends SoporteRolesPago {

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private PublicacionService publicacion;
    @Autowired private GestionRolesPagoService gestion;

    @BeforeEach
    void escenario() {
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);
        persona("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, true);
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));   // María queda sin rol
    }

    @Test
    void elResumenCuentaPersonasPorCedulaYDiceQuienFalta() {
        persona("zz.juan.alterna", CEDULA_JUAN, NOMBRE_JUAN, true);   // segunda cuenta de la misma persona

        ResumenPeriodoDto resumen = publicacion.resumen(PERIODO);

        assertThat(resumen.estadoPeriodo()).isEqualTo("CARGADO");
        assertThat(resumen.rolesActivos()).isEqualTo(1);
        assertThat(resumen.personasActivas()).isEqualTo(2);
        assertThat(resumen.sinRol()).isEqualTo(1);
        assertThat(resumen.nombresSinRol()).containsExactly(NOMBRE_MARIA);
    }

    @Test
    void noSePublicaConFaltantesSinConfirmar() {
        assertThatThrownBy(() -> publicacion.publicar(PERIODO, false, "zz.rrhh"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("sin rol");

        assertThat(publicacion.resumen(PERIODO).estadoPeriodo()).isEqualTo("CARGADO");
    }

    @Test
    void confirmandoLosFaltantesSePublica() {
        PublicacionDto resultado = publicacion.publicar(PERIODO, true, "zz.rrhh");

        assertThat(resultado.estadoPeriodo()).isEqualTo("PUBLICADO");
        assertThat(resultado.publicadoPor()).isEqualTo("zz.rrhh");
        assertThat(resultado.fechaPublicacion()).isNotNull();
        assertThat(resultado.rolesPublicados()).isEqualTo(1);
        assertThat(resultado.sinRol()).isEqualTo(1);
        Periodo guardado = periodoRepository.findByFechaPeriodo(PERIODO.atDay(1)).orElseThrow();
        assertThat(guardado.getEstadoPeriodo()).isEqualTo(EstadoPeriodo.PUBLICADO);
    }

    @Test
    void sinFaltantesNoHaceFaltaConfirmar() {
        cargarUno(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA));

        assertThat(publicacion.publicar(PERIODO, false, "zz.rrhh").sinRol()).isZero();
    }

    @Test
    void sinRolesActivosNoSePublica() {
        gestion.inactivar(PERIODO, rolDe(CEDULA_JUAN).getId());

        assertThatThrownBy(() -> publicacion.publicar(PERIODO, true, "zz.rrhh"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("no tiene roles activos");
    }

    @Test
    void unPeriodoNoSePublicaDosVeces() {
        publicacion.publicar(PERIODO, true, "zz.rrhh");

        assertThatThrownBy(() -> publicacion.publicar(PERIODO, true, "zz.rrhh"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya fue publicado");
    }

    @Test
    void unPeriodoInexistenteDa404() {
        assertThatThrownBy(() -> publicacion.resumen(YearMonth.of(2098, 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> publicacion.publicar(YearMonth.of(2098, 1), true, "zz.rrhh"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}