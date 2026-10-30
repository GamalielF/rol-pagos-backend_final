package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccesoRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.PublicacionService;
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
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmpleadoRolPagoControllerIT extends SoporteRolesPago {

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private WebApplicationContext contexto;
    @Autowired private PublicacionService publicacion;
    @Autowired private AccesoRolPagoRepository accesoRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void escenario() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);
        persona("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, true);
        persona("zz.baja", "1711111110", "BAJA RUIZ PEDRO", false);
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

    private static String cuerpo(String cedula) {
        return "{\"cedula\":\"" + cedula + "\"}";
    }

    @Test
    void sinSesionRecibe401() throws Exception {
        mockMvc.perform(post("/mi-rol-pago/consultar").contentType(MediaType.APPLICATION_JSON).content(cuerpo(CEDULA_JUAN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elTitularRecibeSuRolEnBase64() throws Exception {
        mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion("zz.juan", "EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(CEDULA_JUAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.periodo").value("2099-03"))
                .andExpect(jsonPath("$.content.nombreArchivo").value("rol-pago-2099-03.pdf"))
                .andExpect(jsonPath("$.content.tipoContenido").value("application/pdf"))
                .andExpect(jsonPath("$.content.contenidoBase64").isNotEmpty());
    }

    @Test
    void laCedulaDeUnCompaneroDa403YNoEntregaNada() throws Exception {
        mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion("zz.juan", "EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(CEDULA_MARIA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(content().string(not(containsString("contenidoBase64"))));

        long denegados = accesoRepository.findAll().stream()
                .filter(a -> "zz.juan".equals(a.getUsernameAd()) && a.getResultado() == ResultadoAcceso.DENEGADO)
                .count();
        assertThat(denegados).isEqualTo(1);
    }

    @Test
    void unUsuarioInactivoEnElAdNoObtieneSesion() throws Exception {
        mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion("zz.baja", "EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("1711111110")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unaCuentaSinCedulaRecibe403YSuListaDePeriodosEsVacia() throws Exception {
        mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion("zz.generica", "EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(CEDULA_JUAN)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/mi-rol-pago/periodos").with(sesion("zz.generica", "EMPLEADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void losDatosMalFormadosDan400() throws Exception {
        mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion("zz.juan", "EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("123")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion("zz.juan", "EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"" + CEDULA_JUAN + "\",\"periodo\":\"2099-13\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unPeriodoSinRolPublicadoDa404() throws Exception {
        mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion("zz.juan", "EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"" + CEDULA_JUAN + "\",\"periodo\":\"2099-04\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void losPeriodosDisponiblesSonLosPublicadosDelTitular() throws Exception {
        mockMvc.perform(get("/mi-rol-pago/periodos").with(sesion("zz.juan", "EMPLEADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0]").value("2099-03"));
    }
}
