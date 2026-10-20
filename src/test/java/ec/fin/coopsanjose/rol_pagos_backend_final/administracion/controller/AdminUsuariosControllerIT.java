package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Casi todo se prueba SIN X-Dev-Roles: los roles salen de la configuración (fsepa, msavala) y de la base de datos.
 * Truco para saber si alguien tiene RRHH: /rrhh/periodos/2098-01/resumen da 404 si entra (el período no existe)
 * y 403 si no tiene el rol.
 */
class AdminUsuariosControllerIT extends SoporteRolesPago {

    private static final String BASE = "/admin/usuarios";
    private static final String ZONA_RRHH = "/rrhh/periodos/2098-01/resumen";
    private static final String VER_ROL_AJENO = "/rrhh/periodos/2099-03/roles-pago/999999/ver";
    private static final String MOTIVO = "{\"motivo\":\"Verificacion antes de publicar\"}";

    @Autowired private WebApplicationContext contexto;
    private MockMvc mockMvc;

    @BeforeEach
    void escenario() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        persona("zz.pedro", CEDULA_JUAN, NOMBRE_JUAN, true);
    }

    /** Sesión con los roles REALES (sin cabecera de roles). */
    private static RequestPostProcessor real(String usuario) {
        return peticion -> {
            peticion.addHeader("X-Dev-Usuario", usuario);
            return peticion;
        };
    }

    /** Sesión con roles simulados. */
    private static RequestPostProcessor simulada(String usuario, String roles) {
        return peticion -> {
            peticion.addHeader("X-Dev-Usuario", usuario);
            peticion.addHeader("X-Dev-Roles", roles);
            return peticion;
        };
    }

    @Test
    void sinSesionRecibe401() throws Exception {
        mockMvc.perform(get(BASE + "/privilegiados")).andExpect(status().isUnauthorized());
    }

    @Test
    void rrhhYEmpleadosNoAdministranRoles() throws Exception {
        for (String roles : List.of("RRHH", "EMPLEADO")) {
            mockMvc.perform(get(BASE).with(simulada("zz.pedro", roles))).andExpect(status().isForbidden());
            mockMvc.perform(get(BASE + "/privilegiados").with(simulada("zz.pedro", roles))).andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/zz.pedro/roles/RRHH/otorgar").with(simulada("zz.pedro", roles))).andExpect(status().isForbidden());
            mockMvc.perform(post(BASE + "/zz.pedro/roles/RRHH/revocar").with(simulada("zz.pedro", roles))).andExpect(status().isForbidden());
        }
    }

    @Test
    void elAdministradorDeArranqueEntraConSusRolesRealesSinNingunaCabeceraDeRoles() throws Exception {
        mockMvc.perform(get("/mi-perfil").with(real("fsepa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.roles", hasItems("ADMIN", "EMPLEADO")))
                .andExpect(jsonPath("$.content.permisos", hasItem("VER_ROLES_AJENOS")));

        mockMvc.perform(get(BASE + "/privilegiados").with(real("msavala")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username=='fsepa')].roles[0].origen", hasItem("CONFIGURACION")))
                .andExpect(jsonPath("$.content[?(@.username=='msavala')].roles[0].origen", hasItem("CONFIGURACION")));
    }

    @Test
    void otorgarYRevocarSurtenEfectoEnElSiguienteClic() throws Exception {
        mockMvc.perform(get(ZONA_RRHH).with(real("zz.pedro"))).andExpect(status().isForbidden());

        mockMvc.perform(post(BASE + "/zz.pedro/roles/RRHH/otorgar").with(real("fsepa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.roles[0].rol").value("RRHH"));
        mockMvc.perform(get(ZONA_RRHH).with(real("zz.pedro"))).andExpect(status().isNotFound());   // entra: el período no existe

        mockMvc.perform(post(BASE + "/zz.pedro/roles/RRHH/revocar").with(real("fsepa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.roles").isEmpty());
        mockMvc.perform(get(ZONA_RRHH).with(real("zz.pedro"))).andExpect(status().isForbidden());   // ya no
    }

    @Test
    void elPermisoDeVerRolesAjenosSeAplicaDeVerdad() throws Exception {
        mockMvc.perform(post(BASE + "/zz.pedro/roles/RRHH/otorgar").with(real("fsepa"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verRolesAjenos\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.roles[0].verRolesAjenos").value(false));

        mockMvc.perform(get(ZONA_RRHH).with(real("zz.pedro"))).andExpect(status().isNotFound());   // sigue siendo RRHH
        mockMvc.perform(post(VER_ROL_AJENO).with(real("zz.pedro")).contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isForbidden());                                                // pero no abre roles ajenos

        mockMvc.perform(post(BASE + "/zz.pedro/roles/RRHH/otorgar").with(real("fsepa"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verRolesAjenos\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(VER_ROL_AJENO).with(real("zz.pedro")).contentType(MediaType.APPLICATION_JSON).content(MOTIVO))
                .andExpect(status().isNotFound());                                                 // ahora llega a buscar el rol
    }

    @Test
    void laAuditoriaDiceQuienOtorgoElRol() throws Exception {
        mockMvc.perform(post(BASE + "/zz.pedro/roles/RRHH/otorgar").with(real("fsepa"))).andExpect(status().isOk());

        mockMvc.perform(get("/admin/auditoria/acciones").param("tipo", "USUARIO_ROL_OTORGADO").param("usuario", "fsepa")
                        .with(real("fsepa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.elementos[0].usuario").value("fsepa"))
                .andExpect(jsonPath("$.content.elementos[0].detalle").value("usuario=zz.pedro, rol=RRHH, verRolesAjenos=true"));
    }

    @Test
    void losDatosInvalidosDanSuCodigo() throws Exception {
        mockMvc.perform(post(BASE + "/zz.pedro/roles/EMPLEADO/otorgar").with(real("fsepa"))).andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/zz.pedro/roles/XYZ/otorgar").with(real("fsepa"))).andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/zz.no.existe/roles/RRHH/otorgar").with(real("fsepa"))).andExpect(status().isNotFound());
        mockMvc.perform(post(BASE + "/msavala/roles/ADMIN/revocar").with(real("fsepa"))).andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/zz.pedro/roles/RRHH/revocar").with(real("fsepa"))).andExpect(status().isNotFound());
    }

    @Test
    void buscarAcotaLaPaginaYNuncaMuestraCedulas() throws Exception {
        mockMvc.perform(get(BASE).param("buscar", "zz.pe").param("tamano", "1000").with(real("fsepa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.tamano").value(200))
                .andExpect(jsonPath("$.content.elementos[0].username").value("zz.pedro"))
                .andExpect(content().string(not(containsString(CEDULA_JUAN))));
    }

    @Test
    void miPerfilDiceSiLaCuentaPuedeConsultarSuPropioRol() throws Exception {
        mockMvc.perform(get("/mi-perfil").with(simulada("zz.pedro", "EMPLEADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.puedeConsultarSuRol").value(true))
                .andExpect(jsonPath("$.content.cedulaEnmascarada").value("17******75"))
                .andExpect(content().string(not(containsString(CEDULA_JUAN))));

        mockMvc.perform(get("/mi-perfil").with(simulada("zz.sin.directorio", "EMPLEADO")))   // cuenta sin cédula en el AD
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.puedeConsultarSuRol").value(false))
                .andExpect(jsonPath("$.content.cedulaEnmascarada").doesNotExist());
    }
}
