package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RrhhRolPagoControllerIT extends SoporteRolesPago {

    private static final String BASE = "/rrhh/periodos/2099-03/roles-pago";

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private WebApplicationContext contexto;
    private MockMvc mockMvc;

    @BeforeEach
    void prepararMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        persona("jperez", CEDULA_JUAN, NOMBRE_JUAN, true);
    }

    /** Sesión de desarrollo con los roles indicados. Sirve igual para get, post y multipart. */
    private static RequestPostProcessor como(String roles) {
        return peticion -> {
            peticion.addHeader("X-Dev-Usuario", "fsepa");
            peticion.addHeader("X-Dev-Roles", roles);
            return peticion;
        };
    }

    private void cargarJuan() {
        carga.cargar(PERIODO, List.of(archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN))));
    }

    private void publicarElPeriodo() {
        Periodo periodo = periodoRepository.findByFechaPeriodo(PERIODO.atDay(1)).orElseThrow();
        periodo.setEstadoPeriodo(EstadoPeriodo.PUBLICADO);
        periodoRepository.save(periodo);
        em.flush();
    }

    // ------------------------------------------------------------ seguridad

    @Test
    void sinSesionRecibe401() throws Exception {
        mockMvc.perform(multipart(BASE).file(archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401"));
    }

    @Test
    void unEmpleadoRecibe403() throws Exception {
        mockMvc.perform(multipart(BASE)
                        .file(archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)))
                        .with(como("EMPLEADO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"));
    }

    // ------------------------------------------------------------ carga

    @Test
    void rrhhCargaYRecibeElResumenPorArchivo() throws Exception {
        mockMvc.perform(multipart(BASE)
                        .file(archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)))
                        .file(archivo("rol_malo.pdf", new byte[]{1}))
                        .with(como("RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Carga completada con archivos rechazados"))
                .andExpect(jsonPath("$.content.total").value(2))
                .andExpect(jsonPath("$.content.cargados").value(1))
                .andExpect(jsonPath("$.content.rechazados").value(1))
                .andExpect(jsonPath("$.content.resultados[0].estado").value("CARGADO"))
                .andExpect(jsonPath("$.content.resultados[1].estado").value("RECHAZADO"));
    }

    @Test
    void unPeriodoMalFormadoDa400() throws Exception {
        mockMvc.perform(multipart("/rrhh/periodos/2099-13/roles-pago")
                        .file(archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)))
                        .with(como("RRHH")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    void sinArchivosDa400() throws Exception {
        mockMvc.perform(multipart(BASE).with(como("RRHH")))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ listar, inactivar, activar

    @Test
    void listarMuestraLaCedulaEnmascaradaYNuncaLaCompleta() throws Exception {
        cargarJuan();

        mockMvc.perform(get(BASE).with(como("RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.roles[0].cedulaEnmascarada").value("17******75"))
                .andExpect(jsonPath("$.content.roles[0].nombre").value(NOMBRE_JUAN))
                .andExpect(content().string(not(containsString(CEDULA_JUAN))));
    }

    @Test
    void inactivarYActivarPorId() throws Exception {
        cargarJuan();
        Long id = rolDe(CEDULA_JUAN).getId();

        mockMvc.perform(post(BASE + "/" + id + "/inactivar").with(como("RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.estado").value("INACTIVO"));
        mockMvc.perform(post(BASE + "/" + id + "/activar").with(como("RRHH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.estado").value("ACTIVO"));
    }

    @Test
    void unPeriodoEquivocadoEnLaRutaDa404() throws Exception {
        cargarJuan();
        Long id = rolDe(CEDULA_JUAN).getId();

        mockMvc.perform(post("/rrhh/periodos/2099-04/roles-pago/" + id + "/inactivar").with(como("RRHH")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unPeriodoPublicadoBloqueaLaCargaYLasModificaciones() throws Exception {
        cargarJuan();
        Long id = rolDe(CEDULA_JUAN).getId();
        publicarElPeriodo();

        mockMvc.perform(multipart(BASE)
                        .file(archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)))
                        .with(como("RRHH")))
                .andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/" + id + "/inactivar").with(como("RRHH")))
                .andExpect(status().isConflict());
    }

    @Test
    void listarUnPeriodoInexistenteDa404() throws Exception {
        mockMvc.perform(get("/rrhh/periodos/2098-01/roles-pago").with(como("RRHH")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"));
    }
}