package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.EnvioCorreoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;

/**
 * Envío mediante la API de correo de la cooperativa. Manda un aviso por llamada, en el formato
 * {"notifications":[{...}]}. La API acepta el lote y lo procesa después: un 200 significa "aceptado".
 */
@Slf4j
public class EnviadorCorreoApi implements EnviadorCorreoConVariables {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    record Notificacion(@JsonProperty("channel") String channel,
                        @JsonProperty("destinatario") String destinatario,
                        @JsonProperty("asunto") String asunto,
                        @JsonProperty("mensaje") String mensaje,
                        @JsonProperty("codigoPlantilla") @JsonInclude(JsonInclude.Include.NON_NULL) String codigoPlantilla,
                        @JsonProperty("variables") @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> variables) {
    }

    record SolicitudEnvio(@JsonProperty("notifications") List<Notificacion> notifications) {
    }

    private final CorreoProperties.Api api;
    private final RestClient restClient;
    private final EnvioCorreoProperties envio;

    public EnviadorCorreoApi(CorreoProperties.Api api, RestClient restClient) {
        this(api, restClient, EnvioCorreoProperties.SIN_AJUSTES);
    }

    public EnviadorCorreoApi(CorreoProperties.Api api, RestClient restClient, EnvioCorreoProperties envio) {
        this.api = api;
        this.restClient = restClient;
        this.envio = envio;
        if (envio.redirige()) {
            log.warn("REDIRECCION DE CORREO ACTIVA: todos los avisos saldran a una unica direccion de prueba");
        }
    }

    @Override
    public boolean estaConfigurado() {
        return tieneTexto(api.url());
    }

    @Override
    public void enviar(String destinatario, String asunto, String texto, String referencia) {
        enviar(destinatario, asunto, texto, referencia, Map.of());
    }

    @Override
    public void enviar(String destinatario, String asunto, String texto, String referencia,
                       Map<String, String> variables) {
        if (!estaConfigurado()) {
            throw new EnvioCorreoException("La API de correo no está configurada", true, null);
        }
        String respuesta;
        try {
            respuesta = restClient.post()
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
                    .body(new SolicitudEnvio(List.of(armar(destinatario, asunto, texto, referencia, variables))))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw clasificarEstado(e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw clasificarRed(e);
        } catch (RestClientException e) {
            throw new EnvioCorreoException("La API de correo respondió de forma inesperada", true, e);
        }
        verificarRespuesta(respuesta);
    }

    private Notificacion armar(String destinatario, String asunto, String texto, String referencia,
                               Map<String, String> variablesDelAviso) {
        boolean redirigido = envio.redirige();
        String para = redirigido ? envio.redirigirA().trim() : destinatario;
        String asuntoFinal = redirigido ? "[PRUEBA] " + asunto : asunto;
        String mensaje = redirigido
                ? "Mensaje de prueba (" + referencia + "): el destinatario real no recibió este aviso.\n\n" + texto
                : texto;
        Map<String, String> variables = null;
        if (envio.usaPlantilla()) {
            Map<String, String> mapa = new LinkedHashMap<>();
            Map<String, String> origen = variablesDelAviso == null ? Map.of() : variablesDelAviso;
            // Con redirección, la dirección real del empleado tampoco debe viajar dentro de las variables
            origen.forEach((nombre, valor) ->
                    mapa.put(nombre, redirigido && valor != null && valor.equals(destinatario) ? para : valor));
            mapa.putAll(envio.variables());   // lo fijado en la configuración tiene prioridad
            variables = mapa;
        }
        return new Notificacion(envio.canal(), para, asuntoFinal, mensaje,
                envio.usaPlantilla() ? envio.codigoPlantilla().trim() : null, variables);
    }

    /**
     * La API responde 200 aunque el trabajo se procese después. Solo se puede rechazar lo que ella misma marque
     * como no aceptado; cualquier otra cosa se da por aceptada, porque reintentar un correo ya enviado lo duplica.
     */
    private static void verificarRespuesta(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return;
        }
        JsonNode nodo;
        try {
            nodo = JSON.readTree(cuerpo);
        } catch (RuntimeException e) {
            return;
        }
        JsonNode exito = nodo == null ? null : nodo.get("success");
        if (exito != null && exito.isBoolean() && !exito.booleanValue()) {
            throw new EnvioCorreoException("La API de correo indicó que no aceptó el envío", true, null);
        }
    }

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