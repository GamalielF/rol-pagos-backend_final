package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import com.nimbusds.jwt.JWTClaimsSet;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt.TokensDePrueba;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Solo JWT: el modo de desarrollo (X-Dev-*) está apagado en este contexto. */
@SpringBootTest
class SeguridadJwtIT {

    private static final TokensDePrueba CLAVES = TokensDePrueba.nuevo();

    @DynamicPropertySource
    static void soloJwt(DynamicPropertyRegistry registro) {
        registro.add("app.jwt.clave-publica", CLAVES::clavePublicaPem);
        registro.add("app.seguridad.dev-auth-habilitado", () -> "false");
    }

    @Autowired
    private WebApplicationContext contexto;
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion, String token) {
        return peticion.header("Authorization", "Bearer " + token);
    }

    private void assertSinSesion(MockHttpServletRequestBuilder peticion) throws Exception {
        mockMvc.perform(peticion).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unTokenValidoDaSesionYLosRolesSalenDeLaBaseDeDatos() throws Exception {
        mockMvc.perform(conToken(get("/mi-perfil"), CLAVES.firmar(TokensDePrueba.sesion("fsepa"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.username").value("fsepa"))
                .andExpect(jsonPath("$.content.roles", hasItems("ADMIN", "EMPLEADO")));
    }

    @Test
    void elTokenSirveEnLosEndpointsDeNegocio() throws Exception {
        mockMvc.perform(conToken(get("/mi-rol-pago/periodos"), CLAVES.firmar(TokensDePrueba.sesion("zz.nadie"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void sinCabeceraEs401EnApiResponse() throws Exception {
        assertSinSesion(get("/mi-perfil"));
    }

    @Test
    void lasCabecerasDeDesarrolloSeIgnoranConElModoDevApagado() throws Exception {
        assertSinSesion(get("/mi-perfil").header("X-Dev-Usuario", "fsepa").header("X-Dev-Roles", "ADMIN"));
    }

    @Test
    void unTokenVencidoEs401() throws Exception {
        Instant ahora = Instant.now();
        String vencido = CLAVES.firmar(TokensDePrueba.reclamos("fsepa", ahora.minus(Duration.ofHours(2)),
                ahora.minus(Duration.ofHours(1))));
        assertSinSesion(conToken(get("/mi-perfil"), vencido));
    }

    @Test
    void elTokenTemporalDelPasoMfaEs401() throws Exception {
        String mfa = CLAVES.firmar(TokensDePrueba.sesion("fsepa").claim("type", "MFA_CHALLENGE"));
        assertSinSesion(conToken(get("/mi-perfil"), mfa));
    }

    @Test
    void unTokenConVidaDeRenovacionDe7DiasEs401() throws Exception {
        Instant ahora = Instant.now();
        String largo = CLAVES.firmar(TokensDePrueba.reclamos("fsepa", ahora, ahora.plus(Duration.ofDays(7))));
        assertSinSesion(conToken(get("/mi-perfil"), largo));
    }

    @Test
    void unTokenFirmadoConOtraClaveEs401() throws Exception {
        assertSinSesion(conToken(get("/mi-perfil"), TokensDePrueba.firmarConOtraClave(TokensDePrueba.sesion("fsepa"))));
    }

    @Test
    void unTokenSinFirmaEs401() throws Exception {
        assertSinSesion(conToken(get("/mi-perfil"), TokensDePrueba.sinFirma(TokensDePrueba.sesion("fsepa"))));
    }

    @Test
    void laConfusionDeAlgoritmoEs401() throws Exception {
        String hs256 = TokensDePrueba.hs256(TokensDePrueba.sesion("fsepa"), CLAVES.publica().getEncoded());
        assertSinSesion(conToken(get("/mi-perfil"), hs256));
    }

    @Test
    void lasCookiesNoCuentanComoSesion() throws Exception {
        String token = CLAVES.firmar(TokensDePrueba.sesion("fsepa"));
        assertSinSesion(get("/mi-perfil").cookie(new Cookie("refresh_token", token), new Cookie("access_token", token)));
    }

    @Test
    void otroEsquemaDeAutorizacionEs401() throws Exception {
        assertSinSesion(get("/mi-perfil").header("Authorization", "Basic " + CLAVES.firmar(TokensDePrueba.sesion("fsepa"))));
    }

    @Test
    void unRolDentroDelTokenNoDaAccesoAdministrativo() throws Exception {
        JWTClaimsSet.Builder conRolFalso = TokensDePrueba.sesion("zz.sin.privilegios")
                .claim("roles", java.util.List.of("ADMIN", "RRHH"));
        String token = CLAVES.firmar(conRolFalso);
        mockMvc.perform(conToken(get("/admin/usuarios"), token)).andExpect(status().isForbidden());
        mockMvc.perform(conToken(get("/rrhh/periodos"), token)).andExpect(status().isForbidden());
    }

    @Test
    void elFrontPuedeEnviarLaCabeceraAuthorizationEnElPreflight() throws Exception {
        mockMvc.perform(options("/mi-rol-pago/consultar")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase("authorization")));
    }
}