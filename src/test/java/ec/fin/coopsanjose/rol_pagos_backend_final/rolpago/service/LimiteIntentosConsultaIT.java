package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.DetalleAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccesoRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.AccesoDenegadoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.DemasiadosIntentosException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.config.LimiteIntentosProperties;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quien prueba cédulas ajenas se queda sin consultar durante la ventana configurada. */
class LimiteIntentosConsultaIT extends SoporteRolesPago {

    private static final String IP = "10.0.0.5";

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private PublicacionService publicacion;
    @Autowired private ConsultaRolPagoService consulta;
    @Autowired private AccesoRolPagoRepository accesoRepository;
    @Autowired private LimiteIntentosProperties limite;
    @Autowired private WebApplicationContext contexto;

    @BeforeEach
    void escenario() {
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);
        persona("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, true);
        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA))));
        publicacion.publicar(PERIODO, false, "zz.rrhh");
    }

    private static UsuarioAutenticado juan() {
        return new UsuarioAutenticado("zz.juan", CEDULA_JUAN, null, Set.of(Rol.EMPLEADO));
    }

    private void probarCedulasAjenas(int veces) {
        for (int i = 0; i < veces; i++) {
            assertThatThrownBy(() -> consulta.consultarPropio(juan(), CEDULA_MARIA, null, IP))
                    .isInstanceOf(AccesoDenegadoException.class);
        }
    }

    private void consultarLaPropia() {
        assertThat(consulta.consultarPropio(juan(), CEDULA_JUAN, null, IP).contenidoBase64()).isNotEmpty();
    }

    private long registros(String usuario, DetalleAcceso detalle) {
        em.flush();
        return accesoRepository.findAll().stream()
                .filter(a -> usuario.equals(a.getUsernameAd()) && a.getDetalle() == detalle).count();
    }

    /** Mueve al pasado los intentos fallidos de ese usuario, como si hubieran ocurrido hace 'minutos'. */
    private void envejecerFallidos(String usuario, long minutos) {
        em.flush();
        em.createNativeQuery("UPDATE ACCESOS_ROLES_PAGO SET CREADO_EN = DATE_SUB(CREADO_EN, INTERVAL :minutos MINUTE) "
                        + "WHERE USERNAME_AD = :usuario AND DETALLE = 'CEDULA_NO_COINCIDE'")
                .setParameter("minutos", minutos).setParameter("usuario", usuario).executeUpdate();
        em.clear();
    }

    private long minutosFueraDeLaVentana() {
        return limite.ventana().toMinutes() + 1;
    }

    @Test
    void alLlegarAlMaximoSeBloqueaHastaLaConsultaConSuPropiaCedula() {
        probarCedulasAjenas(limite.maximo());
        assertThatThrownBy(this::consultarLaPropia).isInstanceOf(DemasiadosIntentosException.class);
        assertThat(registros("zz.juan", DetalleAcceso.BLOQUEADO_POR_INTENTOS)).isEqualTo(1);
    }

    @Test
    void unIntentoMenosQueElMaximoNoBloquea() {
        probarCedulasAjenas(limite.maximo() - 1);
        assertThatCode(this::consultarLaPropia).doesNotThrowAnyException();
    }

    @Test
    void losIntentosBloqueadosNoAlarganElBloqueo() {
        probarCedulasAjenas(limite.maximo());
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(this::consultarLaPropia).isInstanceOf(DemasiadosIntentosException.class);
        }
        assertThat(registros("zz.juan", DetalleAcceso.CEDULA_NO_COINCIDE)).isEqualTo(limite.maximo());
        envejecerFallidos("zz.juan", minutosFueraDeLaVentana());
        assertThatCode(this::consultarLaPropia).doesNotThrowAnyException();
    }

    @Test
    void losIntentosFueraDeLaVentanaYaNoCuentan() {
        probarCedulasAjenas(limite.maximo() - 1);
        envejecerFallidos("zz.juan", minutosFueraDeLaVentana());
        probarCedulasAjenas(1);
        assertThatCode(this::consultarLaPropia).doesNotThrowAnyException();
    }

    @Test
    void elBloqueoEsPorUsuarioYNoAfectaAOtros() {
        probarCedulasAjenas(limite.maximo());
        UsuarioAutenticado maria = new UsuarioAutenticado("zz.maria", CEDULA_MARIA, null, Set.of(Rol.EMPLEADO));
        assertThat(consulta.consultarPropio(maria, CEDULA_MARIA, null, IP).contenidoBase64()).isNotEmpty();
    }

    @Test
    void lasConsultasPermitidasNuncaCuentanComoIntentos() {
        for (int i = 0; i < limite.maximo() + 2; i++) {
            consultarLaPropia();
        }
        assertThat(registros("zz.juan", DetalleAcceso.BLOQUEADO_POR_INTENTOS)).isZero();
    }

    @Test
    void porHttpElBloqueoSeVeComo429YNoEntregaNada() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        RequestPostProcessor sesion = peticion -> {
            peticion.addHeader("X-Dev-Usuario", "zz.juan");
            peticion.addHeader("X-Dev-Roles", "EMPLEADO");
            return peticion;
        };
        for (int i = 0; i < limite.maximo(); i++) {
            mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"cedula\":\"" + CEDULA_MARIA + "\"}")).andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/mi-rol-pago/consultar").with(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"" + CEDULA_JUAN + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("429"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(content().string(not(containsString("contenidoBase64"))));
    }
}