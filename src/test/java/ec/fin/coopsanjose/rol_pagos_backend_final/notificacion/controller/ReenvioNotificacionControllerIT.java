package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.SoporteNotificaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReenvioNotificacionControllerIT extends SoporteNotificaciones {

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

    private String ruta(String cedula) {
        return "/rrhh/periodos/2099-03/notificaciones/" + rolDe(cedula).getId() + "/reenviar";
    }

    @Test
    void sinSesionOComoEmpleadoNoSePuedeReenviar() throws Exception {
        publicar();
        despacho.despacharLote();

        mockMvc.perform(post(ruta(CEDULA_JUAN))).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ruta(CEDULA_JUAN)).with(sesion("zz.juan", "EMPLEADO"))).andExpect(status().isForbidden());
    }

    @Test
    void rrhhReenviaYRecibeElResultadoConLaNotificacionActualizada() throws Exception {
        publicar();
        despacho.despacharLote();

        mockMvc.perform(post(ruta(CEDULA_JUAN)).with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.aceptado").value(true))
                .andExpect(jsonPath("$.content.notificacion.estado").value("ENVIADA"))
                .andExpect(jsonPath("$.content.notificacion.reenvios").value(1))
                .andExpect(jsonPath("$.content.notificacion.reenviadaEn").isNotEmpty());
    }

    @Test
    void unAvisoEnColaDevuelveConflicto() throws Exception {
        publicar();

        mockMvc.perform(post(ruta(CEDULA_JUAN)).with(sesion("zz.rrhh", "RRHH"))).andExpect(status().isConflict());
    }

    @Test
    void unPeriodoSinPublicarDevuelveConflicto() throws Exception {
        mockMvc.perform(post(ruta(CEDULA_JUAN)).with(sesion("zz.rrhh", "RRHH"))).andExpect(status().isConflict());
    }

    @Test
    void unRolInexistenteDevuelveNoEncontrado() throws Exception {
        publicar();

        mockMvc.perform(post("/rrhh/periodos/2099-03/notificaciones/999999999/reenviar").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isNotFound());
    }
}
