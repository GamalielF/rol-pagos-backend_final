package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Respuestas de seguridad (401 y 403) en el formato estándar ApiResponse. */
@Slf4j
@Component
public class SeguridadRespuestaHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SeguridadRespuestaHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401: no hay sesión válida. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        log.warn("401 en {} {}", request.getMethod(), request.getRequestURI());
        escribir(response, HttpStatus.UNAUTHORIZED, "Debe autenticarse para acceder a este recurso");
    }

    /** 403: hay sesión, pero el rol no alcanza. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        log.warn("403 en {} {}", request.getMethod(), request.getRequestURI());
        escribir(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(estado, mensaje)));
    }
}
