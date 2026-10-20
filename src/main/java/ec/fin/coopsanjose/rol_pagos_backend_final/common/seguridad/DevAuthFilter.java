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
 * SOLO DESARROLLO. Identifica al usuario por X-Dev-Usuario. Con X-Dev-Roles simula los roles indicados;
 * SIN esa cabecera usa los roles reales (base de datos y administradores iniciales).
 * SecurityConfig lo registra únicamente si app.seguridad.dev-auth-habilitado=true (perfil dev).
 */
@Slf4j
public class DevAuthFilter extends OncePerRequestFilter {

    public static final String CABECERA_USUARIO = "X-Dev-Usuario";
    public static final String CABECERA_ROLES = "X-Dev-Roles";

    private final DirectorioUsuarios directorio;
    private final ResolutorRoles resolutorRoles;

    public DevAuthFilter(DirectorioUsuarios directorio, ResolutorRoles resolutorRoles) {
        this.directorio = directorio;
        this.resolutorRoles = resolutorRoles;
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
                String username = perfil.map(PerfilDirectorio::username).orElse(solicitado);
                String cedula = perfil.map(PerfilDirectorio::cedula).orElse(null);
                String nombre = perfil.map(PerfilDirectorio::nombre).orElse(null);
                String cabeceraRoles = limpiar(request.getHeader(CABECERA_ROLES));

                UsuarioAutenticado usuario;
                if (cabeceraRoles != null) {
                    usuario = new UsuarioAutenticado(username, cedula, nombre, leerRoles(cabeceraRoles));   // simulación
                } else {
                    ResolutorRoles.RolesYPermisos reales = resolutorRoles.resolver(username);               // roles reales
                    usuario = new UsuarioAutenticado(username, cedula, nombre, reales.roles(), reales.permisos());
                }
                List<SimpleGrantedAuthority> autoridades = usuario.roles().stream()
                        .map(rol -> new SimpleGrantedAuthority(rol.autoridad()))
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(usuario, null, autoridades));
            }
        }
        chain.doFilter(request, response);
    }

    /** Roles separados por coma. Los desconocidos se ignoran; si no queda ninguno, EMPLEADO (nunca se escala). */
    private Set<Rol> leerRoles(String cabecera) {
        Set<Rol> roles = EnumSet.noneOf(Rol.class);
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
        if (roles.isEmpty()) {
            roles.add(Rol.EMPLEADO);
        }
        return roles;
    }

    private String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}