package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Valida los tokens de sesión del autenticador: firma RS256 (cualquier otro algoritmo se rechaza), vencimiento,
 * tipo ACCESS, vida máxima y código de usuario. Sin clave configurada queda "no configurado" y no valida nada.
 */
@Slf4j
@Component
public class ValidadorTokenAcceso {

    private static final int LARGO_MAXIMO = 8192;
    private static final Pattern CODIGO_USUARIO = Pattern.compile("[A-Za-z0-9._-]{1,50}");

    private final NimbusJwtDecoder decoder;   // null si no hay clave

    public ValidadorTokenAcceso(JwtProperties propiedades) {
        String texto = leerClave(propiedades);
        if (texto == null) {
            this.decoder = null;
            log.info("JWT: no hay clave pública configurada (app.jwt.clave-publica o clave-publica-archivo): no se validarán tokens");
            return;
        }
        RSAPublicKey clave = ClavePublica.cargar(texto);
        Duration vidaMaxima = propiedades.vidaMaxima();
        NimbusJwtDecoder nuevo = NimbusJwtDecoder.withPublicKey(clave)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        nuevo.setClaimSetConverter(sinFechaDeEmisionInventada());
        nuevo.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                new JwtTimestampValidator(propiedades.desfaseReloj()),
                ValidadorTokenAcceso::validarTipo,
                jwt -> validarVida(jwt, vidaMaxima),
                ValidadorTokenAcceso::validarSujeto));
        this.decoder = nuevo;
        log.info("JWT: validación RS256 activa (vida máxima {}, desfase de reloj {})", vidaMaxima, propiedades.desfaseReloj());
    }

    public boolean estaConfigurado() {
        return decoder != null;
    }

    /** @return el código de usuario (claim sub), en minúsculas. @throws TokenInvalidoException si el token no sirve */
    public String validar(String token) {
        if (decoder == null) {
            throw new TokenInvalidoException("sin_clave_configurada");
        }
        if (token == null || token.isBlank() || token.length() > LARGO_MAXIMO) {
            throw new TokenInvalidoException("formato_invalido");
        }
        try {
            Jwt jwt = decoder.decode(token);
            return jwt.getSubject().trim().toLowerCase(Locale.ROOT);
        } catch (JwtValidationException e) {
            throw new TokenInvalidoException(e.getErrors().stream().findFirst()
                    .map(OAuth2Error::getDescription).orElse("token_invalido"));
        } catch (JwtException e) {
            throw new TokenInvalidoException("firma_o_formato_invalido");
        }
    }

    // ------------------------------------------------------------ reglas propias
    /** Spring inventa iat = exp - 1 s si el token no trae iat; así una vida larga pasaría por "1 segundo". Aquí se conserva la ausencia. */
    private static Converter<Map<String, Object>, Map<String, Object>> sinFechaDeEmisionInventada() {
        Converter<Map<String, Object>, Map<String, Object>> porDefecto =
                MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());
        return brutos -> {
            Map<String, Object> convertidos = new HashMap<>(Objects.requireNonNull(porDefecto.convert(brutos)));
            if (!brutos.containsKey(JwtClaimNames.IAT)) {
                convertidos.remove(JwtClaimNames.IAT);
            }
            return convertidos;
        };
    }
    /** Solo sesiones: sin "type" (tokens actuales) o ACCESS. El token temporal del paso MFA y cualquier otro tipo, fuera. */
    private static OAuth2TokenValidatorResult validarTipo(Jwt jwt) {
        Object tipo = jwt.getClaims().get("type");
        return (tipo == null || "ACCESS".equals(tipo)) ? OAuth2TokenValidatorResult.success()
                : fallo("tipo de token no admitido: solo se aceptan tokens de sesion");
    }

    /** Los tokens de renovación (7 días) no se distinguen por tipo: se rechazan por su vida. */
    private static OAuth2TokenValidatorResult validarVida(Jwt jwt, Duration maxima) {
        Instant emitido = jwt.getIssuedAt();
        Instant vence = jwt.getExpiresAt();
        if (emitido == null || vence == null) {
            return fallo("el token debe traer iat y exp");
        }
        return Duration.between(emitido, vence).compareTo(maxima) > 0
                ? fallo("la vida del token excede el maximo permitido") : OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult validarSujeto(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || !CODIGO_USUARIO.matcher(sub.trim()).matches()) {
            return fallo("sub invalido");
        }
        if (jwt.getClaims().get("username") instanceof String username && !username.equalsIgnoreCase(sub.trim())) {
            return fallo("sub y username no coinciden");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult fallo(String descripcion) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", descripcion, null));
    }

    private static String leerClave(JwtProperties propiedades) {
        if (propiedades.clavePublica() != null && !propiedades.clavePublica().isBlank()) {
            return propiedades.clavePublica();
        }
        String archivo = propiedades.clavePublicaArchivo();
        if (archivo == null || archivo.isBlank()) {
            return null;
        }
        Path ruta = Path.of(archivo.trim());
        if (!Files.isRegularFile(ruta)) {
            return null;
        }
        try {
            return Files.readString(ruta);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la clave pública JWT del archivo configurado", e);
        }
    }
}