package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contra el AD REAL (la transacción se revierte al terminar). */
@SpringBootTest
@Transactional
class AdminDirectorioControllerIT {

    @Autowired private WebApplicationContext contexto;
    @Autowired private EntityManager em;
    @Value("${AD_TEST_USERNAME:}") private String usuarioPrueba;
    @Value("${AD_TEST_CEDULA:}") private String cedulaPrueba;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        em.createNativeQuery("DELETE FROM DIRECTORIO_AD").executeUpdate();
        em.flush();
        em.clear();
    }

    private MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-Dev-Usuario", "fsepa").header("X-Dev-Roles", "ADMIN");
    }

    private void requerirDatosDePrueba() {
        assumeThat(usuarioPrueba).as("Defina AD_TEST_USERNAME en .env").isNotBlank();
        assumeThat(cedulaPrueba).as("Defina AD_TEST_CEDULA en .env").isNotBlank();
    }

    @Test
    void adminSincronizaContraElAdReal() throws Exception {
        requerirDatosDePrueba();

        mockMvc.perform(comoAdmin(post("/admin/directorio/sincronizar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.creados").value(greaterThan(0)));
    }

    @Test
    void elUsuarioSeConsultaConLaCedulaEnmascarada() throws Exception {
        requerirDatosDePrueba();
        mockMvc.perform(comoAdmin(post("/admin/directorio/sincronizar"))).andExpect(status().isOk());

        mockMvc.perform(comoAdmin(get("/admin/directorio/usuarios/" + usuarioPrueba)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.cedulaEnmascarada").value(matchesPattern("\\d{2}\\*{6}\\d{2}")))
                .andExpect(content().string(not(containsString(cedulaPrueba))));
    }

    @Test
    void buscarPorIdentificacionConsultaAlAdEnVivo() throws Exception {
        requerirDatosDePrueba();

        mockMvc.perform(comoAdmin(post("/admin/directorio/buscar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"" + cedulaPrueba + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").exists())
                .andExpect(content().string(not(containsString(cedulaPrueba))));
    }

    @Test
    void unaIdentificacionMalFormadaDa400SinLlamarAlAd() throws Exception {
        mockMvc.perform(comoAdmin(post("/admin/directorio/buscar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    void usuarioInexistenteDa404() throws Exception {
        mockMvc.perform(comoAdmin(get("/admin/directorio/usuarios/no-existe-xyz")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"));
    }
}