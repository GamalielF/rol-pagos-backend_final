package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.AccesoDenegadoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.ConsultaRolPagoService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.PublicacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuditoriaControllerIT extends SoporteRolesPago {

    private static final String BASE = "/admin/auditoria";

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private WebApplicationContext contexto;
    @Autowired private ConsultaRolPagoService consulta;
    @Autowired private PublicacionService publicacion;
    private MockMvc mockMvc;

    @BeforeEach
    void escenario() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);
        persona("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, true);
        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA))));
        publicacion.publicar(PERIODO, false, "zz.rrhh");
    }

    private static RequestPostProcessor sesion(String usuario, String roles) {
        return peticion -> {
            peticion.addHeader("X-Dev-Usuario", usuario);
            peticion.addHeader("X-Dev-Roles", roles);
            return peticion;
        };
    }

    private static UsuarioAutenticado empleado(String username, String cedula) {
        return new UsuarioAutenticado(username, cedula, null, Set.of(Rol.EMPLEADO));
    }

    private void consultaDeJuan() {
        consulta.consultarPropio(empleado("zz.juan", CEDULA_JUAN), CEDULA_JUAN, null, "10.0.0.5");
    }

    private void intentoFallido(String usuario, String cedula) {
        assertThatThrownBy(() -> consulta.consultarPropio(empleado(usuario, CEDULA_JUAN), cedula, null, "10.0.0.5"))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    // ------------------------------------------------------------ seguridad

    @Test
    void sinSesionRecibe401() throws Exception {
        mockMvc.perform(get(BASE + "/accesos")).andExpect(status().isUnauthorized());
    }

    @Test
    void niRrhhNiEmpleadosVenLaAuditoria() throws Exception {
        for (String ruta : List.of("/accesos", "/acciones", "/denegados", "/accesos/exportar")) {
            mockMvc.perform(get(BASE + ruta).with(sesion("zz.rrhh", "RRHH"))).andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + ruta).with(sesion("zz.juan", "EMPLEADO"))).andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------ reportes

    @Test
    void adminListaLosAccesosPaginados() throws Exception {
        consultaDeJuan();

        mockMvc.perform(get(BASE + "/accesos").param("usuario", "zz.juan").param("tamano", "10")
                        .with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.tamano").value(10))
                .andExpect(jsonPath("$.content.totalElementos").value(1))
                .andExpect(jsonPath("$.content.elementos[0].usuario").value("zz.juan"))
                .andExpect(jsonPath("$.content.elementos[0].resultado").value("PERMITIDO"))
                .andExpect(jsonPath("$.content.elementos[0].cedulaEnmascarada").value("17******75"))
                .andExpect(content().string(not(containsString(CEDULA_JUAN))));
    }

    @Test
    void adminListaLasAcciones() throws Exception {
        mockMvc.perform(get(BASE + "/acciones").param("periodo", "2099-03").param("tipo", "CARGA_LOTE")
                        .with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.totalElementos").value(1))
                .andExpect(jsonPath("$.content.elementos[0].tipoAccion").value("CARGA_LOTE"));
    }

    @Test
    void losDenegadosSeConsultanPorUsuario() throws Exception {
        intentoFallido("zz.sondeador", CEDULA_MARIA);
        intentoFallido("zz.sondeador", "1723456784");
        intentoFallido("zz.sondeador", "1711111110");

        mockMvc.perform(get(BASE + "/denegados").param("minimo", "3").with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.usuario=='zz.sondeador')].intentos", hasItem(3)))
                .andExpect(jsonPath("$.content[?(@.usuario=='zz.sondeador')].cedulasDistintas", hasItem(3)));
    }

    @Test
    void losDatosMalFormadosDan400() throws Exception {
        mockMvc.perform(get(BASE + "/accesos").param("resultado", "XYZ").with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/accesos").param("desde", "2099-13-01").with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/accesos").param("periodo", "2099-13").with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/accesos").param("desde", "2099-02-01").param("hasta", "2099-01-01")
                        .with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportarDevuelveElCsvEnBase64() throws Exception {
        consultaDeJuan();

        mockMvc.perform(get(BASE + "/accesos/exportar").param("usuario", "zz.juan").with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.tipoContenido").value("text/csv"))
                .andExpect(jsonPath("$.content.filas").value(1))
                .andExpect(jsonPath("$.content.truncado").value(false))
                .andExpect(jsonPath("$.content.nombreArchivo").value(matchesPattern("auditoria-accesos-\\d{4}-\\d{2}-\\d{2}\\.csv")))
                .andExpect(jsonPath("$.content.contenidoBase64").isNotEmpty());
    }

    // ------------------------------------------------------------ cobertura (RRHH y Admin)

    @Test
    void laCoberturaDiceQuienFaltaPorLeer() throws Exception {
        mockMvc.perform(get("/rrhh/periodos/2099-03/cobertura").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.rolesPublicados").value(2))
                .andExpect(jsonPath("$.content.leidos").value(0))
                .andExpect(jsonPath("$.content.pendientes").value(2));

        consultaDeJuan();

        mockMvc.perform(get("/rrhh/periodos/2099-03/cobertura").with(sesion("zz.admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.leidos").value(1))
                .andExpect(jsonPath("$.content.nombresPendientes[0]").value(NOMBRE_MARIA));

        mockMvc.perform(get("/rrhh/periodos/2099-03/cobertura").with(sesion("zz.juan", "EMPLEADO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void laCoberturaValidaElPeriodo() throws Exception {
        mockMvc.perform(get("/rrhh/periodos/2099-13/cobertura").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/rrhh/periodos/2098-01/cobertura").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isNotFound());
    }
}
