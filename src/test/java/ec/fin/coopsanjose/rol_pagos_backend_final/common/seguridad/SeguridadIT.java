package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SeguridadIT {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder peticion, String roles) {
        return peticion
                .header(DevAuthFilter.CABECERA_USUARIO, "fsepa")
                .header(DevAuthFilter.CABECERA_ROLES, roles);
    }

    @Test
    void saludEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void sinSesionDevuelve401EnApiResponse() throws Exception {
        mockMvc.perform(get("/prueba/ok"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void hastaLasRutasInexistentesExigenSesion() throws Exception {
        mockMvc.perform(get("/ruta-que-no-existe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conSesionPasa() throws Exception {
        mockMvc.perform(como(get("/prueba/ok"), "EMPLEADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void laSesionLlevaLaIdentidadQueSeEnvio() throws Exception {
        mockMvc.perform(como(get("/prueba/yo"), "RRHH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.username").value("fsepa"))
                .andExpect(jsonPath("$.content.roles[0]").value("RRHH"));
    }

    @Test
    void empleadoNoEntraAZonaRrhh() throws Exception {
        mockMvc.perform(como(get("/rrhh/cualquiera"), "EMPLEADO"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rrhhEntraAZonaRrhhYLaRutaInexistenteDa404() throws Exception {
        // Autorizado: llega hasta el despacho de rutas, que responde 404 en ApiResponse
        mockMvc.perform(como(get("/rrhh/cualquiera"), "RRHH"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"));
    }

    @Test
    void adminTambienEntraAZonaRrhh() throws Exception {
        mockMvc.perform(como(get("/rrhh/cualquiera"), "ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rrhhNoEntraAZonaAdmin() throws Exception {
        mockMvc.perform(como(get("/admin/cualquiera"), "RRHH"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"));
    }

    @Test
    void adminEntraAZonaAdmin() throws Exception {
        mockMvc.perform(como(get("/admin/cualquiera"), "ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reglaPorMetodoDenegadaDevuelve403EnApiResponse() throws Exception {
        mockMvc.perform(como(get("/prueba/solo-admin"), "RRHH"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void reglaPorMetodoPermitidaDevuelve200() throws Exception {
        mockMvc.perform(como(get("/prueba/solo-admin"), "ADMIN"))
                .andExpect(status().isOk());
    }
}
