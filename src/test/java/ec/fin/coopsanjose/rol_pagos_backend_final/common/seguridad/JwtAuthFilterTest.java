package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt.TokensDePrueba;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt.ValidadorTokenAcceso;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    private static final TokensDePrueba CLAVES = TokensDePrueba.nuevo();

    private final Map<String, PerfilDirectorio> directorio = new HashMap<>();
    private Set<Rol> rolesReales = Set.of(Rol.EMPLEADO);
    private final JwtAuthFilter filtro = new JwtAuthFilter(
            new ValidadorTokenAcceso(CLAVES.propiedades()),
            username -> Optional.ofNullable(directorio.get(username)),
            username -> new ResolutorRoles.RolesYPermisos(rolesReales, Set.of()));

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private Authentication ejecutar(MockHttpServletRequest peticion) throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        filtro.doFilter(peticion, new MockHttpServletResponse(), cadena);
        assertThat(cadena.getRequest()).as("la petición siempre debe seguir su camino").isNotNull();
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static MockHttpServletRequest conBearer(String token) {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("Authorization", "Bearer " + token);
        return peticion;
    }

    private static String tokenDe(String usuario) {
        return CLAVES.firmar(TokensDePrueba.sesion(usuario));
    }

    private static PerfilDirectorio perfil(boolean activo) {
        return new PerfilDirectorio("jperez", "1712345675", "PEREZ LOPEZ JUAN", "jperez@coopsanjose.fin.ec", activo);
    }

    private static List<String> autoridades(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void unTokenValidoCreaLaSesionConLosDatosDelDirectorio() throws Exception {
        directorio.put("jperez", perfil(true));
        Authentication auth = ejecutar(conBearer(tokenDe("jperez")));
        UsuarioAutenticado usuario = (UsuarioAutenticado) auth.getPrincipal();
        assertThat(usuario.username()).isEqualTo("jperez");
        assertThat(usuario.cedula()).isEqualTo("1712345675");
        assertThat(usuario.nombre()).isEqualTo("PEREZ LOPEZ JUAN");
        assertThat(autoridades(auth)).containsExactly("ROLE_EMPLEADO");
    }

    @Test
    void losRolesSalenDeLaBaseDeDatosNuncaDelToken() throws Exception {
        directorio.put("jperez", perfil(true));
        String conRolFalso = CLAVES.firmar(TokensDePrueba.sesion("jperez").claim("roles", List.of("ADMIN")));
        assertThat(autoridades(ejecutar(conBearer(conRolFalso)))).containsExactly("ROLE_EMPLEADO");
    }

    @Test
    void unCambioDeRolesSeNotaEnLaSiguientePeticion() throws Exception {
        directorio.put("jperez", perfil(true));
        assertThat(autoridades(ejecutar(conBearer(tokenDe("jperez"))))).containsExactly("ROLE_EMPLEADO");
        SecurityContextHolder.clearContext();
        rolesReales = Set.of(Rol.RRHH);
        assertThat(autoridades(ejecutar(conBearer(tokenDe("jperez"))))).containsExactly("ROLE_RRHH");
    }

    @Test
    void elCodigoDeUsuarioDelTokenSeNormalizaAMinusculas() throws Exception {
        directorio.put("jperez", perfil(true));
        UsuarioAutenticado usuario = (UsuarioAutenticado) ejecutar(conBearer(tokenDe("JPEREZ"))).getPrincipal();
        assertThat(usuario.username()).isEqualTo("jperez");
        assertThat(usuario.cedula()).isEqualTo("1712345675");
    }

    @Test
    void unUsuarioDelAutenticadorQueNoEstaEnElDirectorioEntraSinCedula() throws Exception {
        UsuarioAutenticado usuario = (UsuarioAutenticado) ejecutar(conBearer(tokenDe("nuevo"))).getPrincipal();
        assertThat(usuario.username()).isEqualTo("nuevo");
        assertThat(usuario.cedula()).isNull();
        assertThat(usuario.nombre()).isNull();
    }

    @Test
    void unUsuarioInactivoEnElAdNoObtieneSesionAunqueSuTokenSeaValido() throws Exception {
        directorio.put("jperez", perfil(false));
        assertThat(ejecutar(conBearer(tokenDe("jperez")))).isNull();
    }

    @Test
    void unTokenInvalidoNoCreaSesion() throws Exception {
        directorio.put("jperez", perfil(true));
        String deOtraClave = TokensDePrueba.firmarConOtraClave(TokensDePrueba.sesion("jperez"));
        assertThat(ejecutar(conBearer(deOtraClave))).isNull();
    }

    @Test
    void elTokenDelPasoMfaNoCreaSesion() throws Exception {
        String mfa = CLAVES.firmar(TokensDePrueba.sesion("jperez").claim("type", "MFA_CHALLENGE"));
        assertThat(ejecutar(conBearer(mfa))).isNull();
    }

    @Test
    void sinCabeceraNoAutentica() throws Exception {
        assertThat(ejecutar(new MockHttpServletRequest())).isNull();
    }

    @Test
    void otroEsquemaDeAutorizacionSeIgnora() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("Authorization", "Basic " + tokenDe("jperez"));
        assertThat(ejecutar(peticion)).isNull();
    }

    @Test
    void elEsquemaBearerNoDistingueMayusculas() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("Authorization", "bearer " + tokenDe("jperez"));
        assertThat(ejecutar(peticion)).isNotNull();
    }

    @Test
    void lasCookiesNuncaCuentanComoSesion() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setCookies(new Cookie("refresh_token", tokenDe("jperez")), new Cookie("access_token", tokenDe("jperez")));
        assertThat(ejecutar(peticion)).isNull();
    }

    @Test
    void unaSesionYaExistenteNoSeSobrescribe() throws Exception {
        Authentication previa = new TestingAuthenticationToken("otro", "x", "ROLE_EMPLEADO");
        SecurityContextHolder.getContext().setAuthentication(previa);
        assertThat(ejecutar(conBearer(tokenDe("jperez")))).isSameAs(previa);
    }
}