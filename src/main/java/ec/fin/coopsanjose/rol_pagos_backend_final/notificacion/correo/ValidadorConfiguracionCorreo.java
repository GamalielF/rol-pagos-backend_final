package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Revisa la configuración de la API de correo antes de enviar. Cada problema se explica sin repetir el valor revisado:
 * una URL o un token pueden llevar claves y el mensaje termina visible en pantalla.
 */
public final class ValidadorConfiguracionCorreo {

    private ValidadorConfiguracionCorreo() {
    }

    /** @return qué tiene mal la URL, o null si está bien. */
    public static String problemaDeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "La URL de la API de correo está vacía";
        }
        String limpia = url.trim();
        if (limpia.indexOf('"') >= 0 || limpia.indexOf('\'') >= 0) {
            return "La URL de la API de correo lleva comillas: quítelas del .env (CORREO_API_URL=https://... sin comillas)";
        }
        if (limpia.indexOf('{') >= 0 || limpia.indexOf('}') >= 0) {
            return "La URL de la API de correo lleva llaves { }: parece una plantilla de Postman; escriba la dirección real";
        }
        if (limpia.chars().anyMatch(Character::isWhitespace)) {
            return "La URL de la API de correo tiene espacios: debe ser una sola dirección";
        }
        String minuscula = limpia.toLowerCase(Locale.ROOT);
        if (!minuscula.startsWith("https://") && !minuscula.startsWith("http://")) {
            return "La URL de la API de correo debe empezar por https:// (o http://)";
        }
        try {
            String servidor = new URI(limpia).getRawAuthority();
            if (servidor == null || servidor.isBlank()) {
                return "La URL de la API de correo no tiene servidor después de https://";
            }
        } catch (URISyntaxException e) {
            return "La URL de la API de correo no es una dirección válida";
        }
        return null;
    }

    /** @return qué tiene mal el token, o null si está bien (o no hay token). */
    public static String problemaDeToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if ((c < 0x20 && c != '\t') || c == 0x7f || c > 0xff) {
                return "El token de la API de correo tiene caracteres no permitidos (saltos de línea u otros símbolos): revise su valor";
            }
        }
        return null;
    }
}
