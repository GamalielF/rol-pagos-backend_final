package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SOLO DESARROLLO. Identifica al usuario por cabeceras X-Dev-* mientras no existe el login real.
 * SecurityConfig lo registra únicamente si app.seguridad.dev-auth-habilitado=true (perfil dev).
 * ETAPA 7: la cédula dejará de venir de la cabecera y saldrá del directorio del AD.
 */
@Slf4j
public class DevAuthFilter extends OncePerRequestFilter {

    public static final String CABECERA_USUARIO = "X-Dev-Usuario";
    public static final String CABECERA_ROLES = "X-Dev-Roles";
    public static final String CABECERA_CEDULA = "X-Dev-Cedula";
    public static final String CABECERA_NOMBRE = "X-Dev-Nombre";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String username = limpiar(request.getHeader(CABECERA_USUARIO));

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Set<Rol> roles = leerRoles(request.getHeader(CABECERA_ROLES));
            UsuarioAutenticado usuario = new UsuarioAutenticado(username,
                    limpiar(request.getHeader(CABECERA_CEDULA)),
                    limpiar(request.getHeader(CABECERA_NOMBRE)),
                    roles);
            List<SimpleGrantedAuthority> autoridades = roles.stream()
                    .map(rol -> new SimpleGrantedAuthority(rol.autoridad()))
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(usuario, null, autoridades));
        }
        chain.doFilter(request, response);
    }

    /** Roles separados por coma. Los desconocidos se ignoran; si no queda ninguno, EMPLEADO. */
    private Set<Rol> leerRoles(String cabecera) {
        Set<Rol> roles = EnumSet.noneOf(Rol.class);
        if (cabecera != null) {
            for (String texto : cabecera.split(",")) {
                String nombre = texto.trim().toUpperCase(Locale.ROOT);
                if (nombre.isEmpty()) {
                    continue;
                }
                try {
                    roles.add(Rol.valueOf(nombre));
                } catch (IllegalArgumentException e) {
                    log.warn("Rol desconocido ignorado: {}", nombre);
                }
            }
        }
        if (roles.isEmpty()) {
            roles.add(Rol.EMPLEADO);
        }
        return roles;
    }

    private String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
