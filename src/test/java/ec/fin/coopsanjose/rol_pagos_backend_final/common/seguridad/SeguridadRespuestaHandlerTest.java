package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SeguridadRespuestaHandlerTest {

    private final SeguridadRespuestaHandler handler = new SeguridadRespuestaHandler(JsonMapper.builder().build());

    @Test
    void sinSesionDevuelve401EnFormatoApiResponse() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), respuesta, new BadCredentialsException("detalle-interno-secreto"));

        String cuerpo = respuesta.getContentAsString();
        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentType()).startsWith("application/json");
        assertThat(JsonPath.<String>read(cuerpo, "$.status")).isEqualTo("401");
        assertThat(JsonPath.<Boolean>read(cuerpo, "$.success")).isFalse();
        assertThat(JsonPath.<String>read(cuerpo, "$.message")).isEqualTo("Debe autenticarse para acceder a este recurso");
        assertThat(cuerpo).doesNotContain("secreto");
    }

    @Test
    void sinPermisoDevuelve403EnFormatoApiResponse() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), respuesta, new AccessDeniedException("detalle-interno-secreto"));

        String cuerpo = respuesta.getContentAsString();
        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(JsonPath.<String>read(cuerpo, "$.status")).isEqualTo("403");
        assertThat(JsonPath.<Boolean>read(cuerpo, "$.success")).isFalse();
        assertThat(cuerpo).doesNotContain("secreto");
    }
}
