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
import java.util.Optional;
import java.util.Set;

/**
 * SOLO DESARROLLO. Identifica al usuario por X-Dev-Usuario y X-Dev-Roles mientras no exista el login real.
 * SecurityConfig lo registra únicamente si app.seguridad.dev-auth-habilitado=true (perfil dev).
 */
@Slf4j
public class DevAuthFilter extends OncePerRequestFilter {

    public static final String CABECERA_USUARIO = "X-Dev-Usuario";
    public static final String CABECERA_ROLES = "X-Dev-Roles";

    private final DirectorioUsuarios directorio;

    public DevAuthFilter(DirectorioUsuarios directorio) {
        this.directorio = directorio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String solicitado = limpiar(request.getHeader(CABECERA_USUARIO));

        if (solicitado != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<PerfilDirectorio> perfil = directorio.buscarPorUsername(solicitado);

            if (perfil.isPresent() && !perfil.get().activo()) {
                // Deshabilitado en el AD: sin sesión (la petición sigue anónima y recibe 401)
                log.warn("Usuario inactivo en el directorio: se rechaza la sesión de desarrollo");
            } else {
                Set<Rol> roles = leerRoles(request.getHeader(CABECERA_ROLES));
                UsuarioAutenticado usuario = new UsuarioAutenticado(
                        perfil.map(PerfilDirectorio::username).orElse(solicitado),
                        perfil.map(PerfilDirectorio::cedula).orElse(null),
                        perfil.map(PerfilDirectorio::nombre).orElse(null),
                        roles);
                List<SimpleGrantedAuthority> autoridades = roles.stream()
                        .map(rol -> new SimpleGrantedAuthority(rol.autoridad()))
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(usuario, null, autoridades));
            }
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