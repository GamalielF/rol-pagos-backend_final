package ec.fin.coopsanjose.rol_pagos_backend_final.common.config;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.DevAuthFilter;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.DirectorioUsuarios;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.JwtAuthFilter;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.ResolutorRoles;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.SeguridadRespuestaHandler;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt.ValidadorTokenAcceso;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // habilita @PreAuthorize para las reglas por metodo
public class SecurityConfig {

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http,
                                                 SeguridadRespuestaHandler respuestas,
                                                 Environment entorno,
                                                 DirectorioUsuarios directorio,
                                                 ResolutorRoles resolutorRoles,
                                                 ValidadorTokenAcceso validadorToken,
                                                 @Value("${app.seguridad.dev-auth-habilitado:false}") boolean devAuth)
            throws Exception {
        // Seguro anti-accidentes: el filtro de desarrollo jamás puede convivir con producción
        if (devAuth && entorno.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "app.seguridad.dev-auth-habilitado no puede estar activo con el perfil prod");
        }
        boolean conJwt = validadorToken.estaConfigurado();
        if (!conJwt && !devAuth) {
            // Sin esto el sistema arrancaría "abierto" a nadie: mejor que no arranque
            throw new IllegalStateException(
                    "No hay forma de autenticar usuarios: configure app.jwt.clave-publica (producción) o active el modo de desarrollo");
        }

        http
                .cors(Customizer.withDefaults())      // orígenes permitidos: CorsConfig (app.cors.*)
                .csrf(AbstractHttpConfigurer::disable)   // sin cookies de sesión: la identidad viaja en la cabecera Authorization
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)    // 401
                        .accessDeniedHandler(respuestas))        // 403
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/rrhh/**").hasAnyRole("RRHH", "ADMIN")
                        .anyRequest().authenticated());          // cerrado por defecto

        if (conJwt) {
            log.info("Autenticación por token JWT activa");
            http.addFilterBefore(new JwtAuthFilter(validadorToken, directorio, resolutorRoles),
                    UsernamePasswordAuthenticationFilter.class);
        }
        if (devAuth) {
            log.warn("AUTENTICACION DE DESARROLLO ACTIVA: se acepta la identidad enviada en las cabeceras X-Dev-*");
            http.addFilterBefore(new DevAuthFilter(directorio, resolutorRoles), UsernamePasswordAuthenticationFilter.class);
        } else {
            log.info("Autenticacion de desarrollo desactivada");
        }
        return http.build();
    }

    /**
     * Este sistema no autentica con usuario y clave locales: la identidad vendrá del AD.
     * Declarar este bean evita que Spring Boot cree un usuario con una clave generada.
     */
    @Bean
    public UserDetailsService sinUsuariosLocales() {
        return username -> {
            throw new UsernameNotFoundException("No hay usuarios locales");
        };
    }
}