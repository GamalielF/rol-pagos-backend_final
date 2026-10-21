package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt.TokenInvalidoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt.ValidadorTokenAcceso;
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
import java.util.List;
import java.util.Optional;

/**
 * Sesión a partir del token Bearer del autenticador. Solo lee la cabecera Authorization: las cookies se ignoran
 * (la de renovación nunca es una sesión para esta API). Los roles NO vienen del token: salen de nuestra base de
 * datos en cada petición, así que quitar un rol o dar de baja a alguien en el AD surte efecto al instante.
 */
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String ESQUEMA = "Bearer ";

    private final ValidadorTokenAcceso validador;
    private final DirectorioUsuarios directorio;
    private final ResolutorRoles resolutorRoles;

    public JwtAuthFilter(ValidadorTokenAcceso validador, DirectorioUsuarios directorio, ResolutorRoles resolutorRoles) {
        this.validador = validador;
        this.directorio = directorio;
        this.resolutorRoles = resolutorRoles;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.regionMatches(true, 0, ESQUEMA, 0, ESQUEMA.length())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = validador.validar(cabecera.substring(ESQUEMA.length()).trim());
                Optional<PerfilDirectorio> perfil = directorio.buscarPorUsername(username);
                if (perfil.isPresent() && !perfil.get().activo()) {
                    log.warn("Token válido de un usuario inactivo en el AD: se rechaza la sesión");
                } else {
                    // Un usuario que el autenticador aceptó pero que aún no está en el directorio entra SIN cédula
                    ResolutorRoles.RolesYPermisos reales = resolutorRoles.resolver(username);
                    UsuarioAutenticado usuario = new UsuarioAutenticado(
                            perfil.map(PerfilDirectorio::username).orElse(username),
                            perfil.map(PerfilDirectorio::cedula).orElse(null),
                            perfil.map(PerfilDirectorio::nombre).orElse(null),
                            reales.roles(), reales.permisos());
                    List<SimpleGrantedAuthority> autoridades = usuario.roles().stream()
                            .map(rol -> new SimpleGrantedAuthority(rol.autoridad()))
                            .toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(usuario, null, autoridades));
                }
            } catch (TokenInvalidoException e) {
                log.warn("Token rechazado: {}", e.categoria());   // sin sesión: la petición seguirá anónima y recibirá 401
            }
        }
        chain.doFilter(request, response);
    }
}