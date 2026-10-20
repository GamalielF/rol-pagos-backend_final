package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import com.fasterxml.jackson.annotation.JsonProperty;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;

/**
 * Envío mediante la API de correo de la cooperativa.
 * ESQUELETO: la conexión, la autenticación, los tiempos de espera y la clasificación de errores son definitivos;
 * lo que depende del contrato de la API está marcado con "AJUSTAR". No hace nada hasta que se configure app.correo.api.url.
 */
public class EnviadorCorreoApi implements EnviadorCorreo {

    /**
     * AJUSTAR: nombres y tipos de los campos según el contrato real de la API
     * (por ejemplo "destinatario" en vez de "para", un campo "html", un identificador de plantilla...).
     */
    record SolicitudEnvio(@JsonProperty("para") String para,
                          @JsonProperty("asunto") String asunto,
                          @JsonProperty("mensaje") String mensaje) {
    }

    private final CorreoProperties.Api api;
    private final RestClient restClient;

    public EnviadorCorreoApi(CorreoProperties.Api api, RestClient restClient) {
        this.api = api;
        this.restClient = restClient;
    }

    @Override
    public boolean estaConfigurado() {
        return tieneTexto(api.url());
    }

    @Override
    public void enviar(String destinatario, String asunto, String texto, String referencia) {
        if (!estaConfigurado()) {
            throw new EnvioCorreoException("La API de correo no está configurada", true, null);
        }
        try {
            restClient.post()
                    .uri(api.url().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(cabeceras -> {
                        if (tieneTexto(api.token())) {
                            cabeceras.set(api.cabeceraAutorizacion(),
                                    tieneTexto(api.esquema()) ? api.esquema() + " " + api.token() : api.token());
                        }
                        if (tieneTexto(api.cabeceraIdempotencia()) && referencia != null) {
                            cabeceras.set(api.cabeceraIdempotencia(), referencia);   // evita duplicados al reintentar
                        }
                    })
                    .body(new SolicitudEnvio(destinatario, asunto, texto))
                    .retrieve()
                    .toBodilessEntity();   // AJUSTAR: si la API responde 200 con un indicador de error en el cuerpo, leerlo aquí
        } catch (RestClientResponseException e) {
            throw clasificarEstado(e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw clasificarRed(e);
        } catch (RestClientException e) {
            throw new EnvioCorreoException("La API de correo respondió de forma inesperada", true, e);
        }
    }

    /** AJUSTAR: afinar según los códigos que documente la API. */
    private static EnvioCorreoException clasificarEstado(int codigo, Exception causa) {
        if (codigo == 401 || codigo == 403) {
            return new EnvioCorreoException("La API de correo rechazó las credenciales (HTTP " + codigo + ")", true, causa);
        }
        if (codigo == 404) {
            return new EnvioCorreoException("La ruta de la API de correo no existe (HTTP 404)", true, causa);
        }
        if (codigo == 408 || codigo == 429) {
            return new EnvioCorreoException("La API de correo pidió esperar (HTTP " + codigo + ")", true, causa);
        }
        if (codigo >= 500) {
            return new EnvioCorreoException("La API de correo respondió con un error interno (HTTP " + codigo + ")", true, causa);
        }
        // Otros 4xx (400, 422...): la solicitud o la dirección son inválidas; repetirla no la arregla
        return new EnvioCorreoException("La API de correo rechazó la solicitud (HTTP " + codigo + ")", false, causa);
    }

    private static EnvioCorreoException clasificarRed(Throwable error) {
        if (tiene(error, SSLException.class)) {
            return new EnvioCorreoException("No se pudo establecer una conexión segura con la API de correo", true, error);
        }
        if (tiene(error, HttpTimeoutException.class, SocketTimeoutException.class)) {
            return new EnvioCorreoException("La API de correo no respondió a tiempo", true, error);
        }
        if (tiene(error, ConnectException.class, UnknownHostException.class)) {
            return new EnvioCorreoException("No se pudo conectar con la API de correo", true, error);
        }
        return new EnvioCorreoException("Error de comunicación con la API de correo", true, error);
    }

    private static boolean tiene(Throwable raiz, Class<?>... tipos) {
        for (Throwable t = raiz; t != null; t = t.getCause()) {
            for (Class<?> tipo : tipos) {
                if (tipo.isInstance(t)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
