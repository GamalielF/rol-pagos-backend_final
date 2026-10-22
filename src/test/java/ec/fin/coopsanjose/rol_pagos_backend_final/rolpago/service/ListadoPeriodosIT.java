package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PeriodoResumenDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El listado de períodos de RRHH: del más reciente al más antiguo, con estado y roles activos. */
class ListadoPeriodosIT extends SoporteRolesPago {

    private static final YearMonth ABRIL = YearMonth.of(2099, 4);

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private ListadoPeriodosService listado;
    @Autowired private PublicacionService publicacion;
    @Autowired private WebApplicationContext contexto;

    @BeforeEach
    void escenario() {
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);
        persona("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, true);
        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA))));
        carga.cargar(ABRIL, List.of(archivo(CEDULA_JUAN + ".pdf", rol("ABRIL 2099", NOMBRE_JUAN))));
    }

    /** Solo los períodos de la prueba: la base puede tener períodos reales. */
    private List<PeriodoResumenDto> deLaPrueba() {
        em.flush();
        return listado.listar().stream().filter(p -> p.periodo().startsWith("2099-")).toList();
    }

    private static RequestPostProcessor sesion(String usuario, String roles) {
        return peticion -> {
            peticion.addHeader("X-Dev-Usuario", usuario);
            peticion.addHeader("X-Dev-Roles", roles);
            return peticion;
        };
    }

    @Test
    void listaDelMasRecienteAlMasAntiguoConSusRolesActivos() {
        assertThat(deLaPrueba())
                .extracting(PeriodoResumenDto::periodo, PeriodoResumenDto::rolesActivos, PeriodoResumenDto::estado)
                .containsExactly(tuple("2099-04", 1L, "CARGADO"), tuple("2099-03", 2L, "CARGADO"));
    }

    @Test
    void unRolInactivoNoCuenta() {
        RolPago deMaria = rolDe(CEDULA_MARIA);
        deMaria.inactivar();
        rolPagoRepository.save(deMaria);
        assertThat(deLaPrueba()).filteredOn(p -> p.periodo().equals("2099-03"))
                .singleElement().extracting(PeriodoResumenDto::rolesActivos).isEqualTo(1L);
    }

    @Test
    void unPeriodoPublicadoMuestraQuienLoPublicoYCuando() {
        publicacion.publicar(PERIODO, false, "zz.rrhh");
        assertThat(deLaPrueba()).filteredOn(p -> p.periodo().equals("2099-03")).singleElement().satisfies(p -> {
            assertThat(p.estado()).isEqualTo("PUBLICADO");
            assertThat(p.publicadoPor()).isEqualTo("zz.rrhh");
            assertThat(p.fechaPublicacion()).isNotNull();
        });
        assertThat(deLaPrueba()).filteredOn(p -> p.periodo().equals("2099-04"))
                .singleElement().extracting(PeriodoResumenDto::estado).isEqualTo("CARGADO");
    }

    @Test
    void rrhhVeElListadoPorHttp() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        em.flush();
        mockMvc.perform(get("/rrhh/periodos").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.periodo=='2099-03')].rolesActivos", contains(2)));
    }

    @Test
    void unEmpleadoNoPuedeVerElListado() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        mockMvc.perform(get("/rrhh/periodos").with(sesion("zz.juan", "EMPLEADO"))).andExpect(status().isForbidden());
    }

    @Test
    void sinSesionEs401() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        mockMvc.perform(get("/rrhh/periodos")).andExpect(status().isUnauthorized());
    }
}