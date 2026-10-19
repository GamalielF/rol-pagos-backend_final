package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ListadoPeriodoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoResumenDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionRolesPagoServiceIT extends SoporteRolesPago {

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private GestionRolesPagoService gestion;

    @BeforeEach
    void unRolCargado() {
        persona("jperez", CEDULA_JUAN, NOMBRE_JUAN, true);
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
    }

    @Test
    void listarMuestraElNombreYLaCedulaEnmascarada() {
        ListadoPeriodoDto listado = gestion.listar(PERIODO);

        assertThat(listado.periodo()).isEqualTo("2099-03");
        assertThat(listado.estadoPeriodo()).isEqualTo("CARGADO");
        assertThat(listado.activos()).isEqualTo(1);
        assertThat(listado.roles()).hasSize(1);
        RolPagoResumenDto fila = listado.roles().get(0);
        assertThat(fila.cedulaEnmascarada()).isEqualTo("17******75");
        assertThat(fila.nombre()).isEqualTo(NOMBRE_JUAN);
        assertThat(listado.toString()).doesNotContain(CEDULA_JUAN);
    }

    @Test
    void inactivarYActivarCambianElEstado() {
        Long id = rolDe(CEDULA_JUAN).getId();

        assertThat(gestion.inactivar(PERIODO, id).estado()).isEqualTo("INACTIVO");
        assertThat(gestion.listar(PERIODO).inactivos()).isEqualTo(1);
        assertThat(gestion.activar(PERIODO, id).estado()).isEqualTo("ACTIVO");
    }

    @Test
    void unRolNoSePuedeTocarDesdeOtroPeriodo() {
        Long id = rolDe(CEDULA_JUAN).getId();

        assertThatThrownBy(() -> gestion.inactivar(YearMonth.of(2099, 4), id))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void unPeriodoPublicadoNoSeModifica() {
        Long id = rolDe(CEDULA_JUAN).getId();
        Periodo periodo = periodoRepository.findByFechaPeriodo(PERIODO.atDay(1)).orElseThrow();
        periodo.setEstadoPeriodo(EstadoPeriodo.PUBLICADO);
        periodoRepository.save(periodo);
        em.flush();

        assertThatThrownBy(() -> gestion.inactivar(PERIODO, id)).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> gestion.activar(PERIODO, id)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void listarUnPeriodoQueNoExisteDa404() {
        assertThatThrownBy(() -> gestion.listar(YearMonth.of(2098, 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
