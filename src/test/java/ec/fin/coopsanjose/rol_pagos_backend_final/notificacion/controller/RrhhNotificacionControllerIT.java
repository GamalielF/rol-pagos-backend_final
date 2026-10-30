package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.SoporteNotificaciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RrhhNotificacionControllerIT extends SoporteNotificaciones {

    private static final String BASE = "/rrhh/periodos/2099-03/notificaciones";

    @Autowired private WebApplicationContext contexto;
    private MockMvc mockMvc;

    @BeforeEach
    void escenario() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        prepararTresPersonas();
    }

    private static RequestPostProcessor sesion(String usuario, String roles) {
        return peticion -> {
            peticion.addHeader("X-Dev-Usuario", usuario);
            peticion.addHeader("X-Dev-Roles", roles);
            return peticion;
        };
    }

    @Test
    void laSeguridadProtegeLosTresEndpoints() throws Exception {
        for (String ruta : List.of(BASE, BASE + "/generar", BASE + "/reintentar")) {
            boolean esConsulta = ruta.equals(BASE);
            mockMvc.perform(esConsulta ? get(ruta) : post(ruta)).andExpect(status().isUnauthorized());
            mockMvc.perform((esConsulta ? get(ruta) : post(ruta)).with(sesion("zz.juan", "EMPLEADO")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void publicarDevuelveLosAvisosEncolados() throws Exception {
        mockMvc.perform(post("/rrhh/periodos/2099-03/publicar").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.avisosEncolados").value(3));
    }

    @Test
    void elInformeResumeLaCola() throws Exception {
        publicar();

        mockMvc.perform(get(BASE).with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.habilitadas").value(true))
                .andExpect(jsonPath("$.content.total").value(3))
                .andExpect(jsonPath("$.content.pendientes").value(3))
                .andExpect(jsonPath("$.content.enviadas").value(0))
                .andExpect(jsonPath("$.content.problemas").isEmpty());
    }

    @Test
    void generarEsIdempotente() throws Exception {
        publicar();

        mockMvc.perform(post(BASE + "/generar").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.creadas").value(0))
                .andExpect(jsonPath("$.content.yaExistentes").value(3));
    }

    @Test
    void reintentarDevuelveSoloLosFallidos() throws Exception {
        publicar();
        mockMvc.perform(post(BASE + "/reintentar").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.reintentadas").value(0));

        NotificacionRolPago fallido = avisoDe(CEDULA_JUAN);
        fallido.setEstado(EstadoNotificacion.FALLIDA);
        fallido.setIntentos(5);
        fallido.setUltimoError("No se pudo conectar con la API de correo");
        avisoRepository.save(fallido);
        em.flush();

        mockMvc.perform(post(BASE + "/reintentar").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.reintentadas").value(1));
    }

    @Test
    void losDatosMalFormadosYLosEstadosInvalidosDanSuCodigo() throws Exception {
        mockMvc.perform(get("/rrhh/periodos/2099-13/notificaciones").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/rrhh/periodos/2098-01/notificaciones").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(BASE + "/generar").with(sesion("zz.rrhh", "RRHH")))   // cargado, sin publicar
                .andExpect(status().isConflict());
    }
}
