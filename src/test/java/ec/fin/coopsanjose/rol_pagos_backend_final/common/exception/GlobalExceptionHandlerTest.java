package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.prueba.PruebaFormatoController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PruebaFormatoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void exitoUsaElFormatoEstandar() throws Exception {
        mockMvc.perform(get("/prueba/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content.periodo").value("2026-10"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void recursoNoEncontradoDevuelve404() throws Exception {
        mockMvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El periodo 2099-01 no existe"))
                .andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test
    void conflictoDevuelve409() throws Exception {
        mockMvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void operacionNoPermitidaDevuelve409() throws Exception {
        mockMvc.perform(get("/prueba/no-permitido"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"));
    }

    @Test
    void bloqueoOptimistaDevuelve409() throws Exception {
        mockMvc.perform(get("/prueba/bloqueo-optimista"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"));
    }

    @Test
    void validacionDevuelve400ConListaDeErrores() throws Exception {
        mockMvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0]").value("cantidad: La cantidad debe ser al menos 1"))
                .andExpect(jsonPath("$.errors[1]").value("nombre: El nombre es obligatorio"));
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    void parametroConTipoIncorrectoDevuelve400() throws Exception {
        mockMvc.perform(get("/prueba/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    void metodoNoSoportadoDevuelve405() throws Exception {
        mockMvc.perform(post("/prueba/ok"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("405"));
    }

    @Test
    void errorInesperadoDevuelve500SinFiltrarDetalles() throws Exception {
        mockMvc.perform(get("/prueba/error-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("500"))
                .andExpect(jsonPath("$.message").value(startsWith("Error interno del servidor. Referencia: ")))
                .andExpect(content().string(not(containsString("secreto"))));
    }
}
