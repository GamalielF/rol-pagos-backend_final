package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccesoRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RrhhPublicacionControllerIT extends SoporteRolesPago {

    private static final String BASE = "/rrhh/periodos/2099-03";
    private static final String CEDULA_LUIS = "1723456784";
    private static final String NOMBRE_LUIS = "SILVA TORRES LUIS ALBERTO";

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private WebApplicationContext contexto;
    @Autowired private AccesoRolPagoRepository accesoRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void escenario() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);
        persona("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, true);
        persona("zz.rrhh", CEDULA_LUIS, NOMBRE_LUIS, true);
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));   // María y Luis quedan sin rol
    }

    private static RequestPostProcessor sesion(String usuario, String roles) {
        return peticion -> {
            peticion.addHeader("X-Dev-Usuario", usuario);
            peticion.addHeader("X-Dev-Roles", roles);
            return peticion;
        };
    }

    @Test
    void elResumenEsSoloParaPersonal() throws Exception {
        mockMvc.perform(get(BASE + "/resumen").with(sesion("zz.juan", "EMPLEADO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/resumen").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.rolesActivos").value(1))
                .andExpect(jsonPath("$.content.sinRol").value(2));
    }

    @Test
    void publicarConFaltantesExigeConfirmacion() throws Exception {
        mockMvc.perform(post(BASE + "/publicar").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("sin rol")));

        mockMvc.perform(post(BASE + "/publicar").with(sesion("zz.rrhh", "RRHH"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmarFaltantes\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.estadoPeriodo").value("PUBLICADO"))
                .andExpect(jsonPath("$.content.publicadoPor").value("zz.rrhh"));
    }

    @Test
    void sinFaltantesSePublicaSinCuerpo() throws Exception {
        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA)),
                archivo(CEDULA_LUIS + ".pdf", rol(MES_PERIODO, NOMBRE_LUIS))));

        mockMvc.perform(post(BASE + "/publicar").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.estadoPeriodo").value("PUBLICADO"));
    }

    @Test
    void verExigeUnMotivoValido() throws Exception {
        Long id = rolDe(CEDULA_JUAN).getId();

        mockMvc.perform(post(BASE + "/roles-pago/" + id + "/ver").with(sesion("zz.rrhh", "RRHH")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/roles-pago/" + id + "/ver").with(sesion("zz.rrhh", "RRHH"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"corto\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verConMotivoDevuelveElPdfYQuedaRegistrado() throws Exception {
        Long id = rolDe(CEDULA_JUAN).getId();

        mockMvc.perform(post(BASE + "/roles-pago/" + id + "/ver").with(sesion("zz.rrhh", "RRHH"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Reclamo del empleado por diferencia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.contenidoBase64").isNotEmpty());

        assertThat(accesoRepository.findAll().stream().filter(a -> "zz.rrhh".equals(a.getUsernameAd())).toList())
                .singleElement().satisfies(a -> {
                    assertThat(a.getTipoAcceso()).isEqualTo(TipoAcceso.CONSULTA_AJENA);
                    assertThat(a.getResultado()).isEqualTo(ResultadoAcceso.PERMITIDO);
                    assertThat(a.getMotivo()).isEqualTo("Reclamo del empleado por diferencia");
                });
    }

    @Test
    void unEmpleadoNoPuedeVerYLaSeguridadLoCortaAntesDelServicio() throws Exception {
        Long id = rolDe(CEDULA_JUAN).getId();

        mockMvc.perform(post(BASE + "/roles-pago/" + id + "/ver").with(sesion("zz.maria", "EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Curiosidad sobre el sueldo\"}"))
                .andExpect(status().isForbidden());

        assertThat(accesoRepository.findAll().stream().filter(a -> "zz.maria".equals(a.getUsernameAd())).toList())
                .isEmpty();
    }

    @Test
    void elIdDeOtroPeriodoDa404() throws Exception {
        Long id = rolDe(CEDULA_JUAN).getId();

        mockMvc.perform(post("/rrhh/periodos/2099-04/roles-pago/" + id + "/ver").with(sesion("zz.rrhh", "RRHH"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo de la consulta\"}"))
                .andExpect(status().isNotFound());
    }
}
