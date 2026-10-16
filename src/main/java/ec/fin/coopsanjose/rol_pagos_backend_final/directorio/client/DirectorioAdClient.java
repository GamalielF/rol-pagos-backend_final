package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ServicioExternoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config.AdProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto.RespuestaAdDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto.UsuarioAdDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateExpiredException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class DirectorioAdClient {

    /** Parámetro de búsqueda del AD. Según el ejemplo del AD, recibe la cédula. */
    static final String PARAMETRO_BUSQUEDA = "codUsuario";

    private final RestClient restClient;
    private final AdProperties propiedades;

    public DirectorioAdClient(@Qualifier("directorioAdRestClient") RestClient restClient, AdProperties propiedades) {
        this.restClient = restClient;
        this.propiedades = propiedades;
    }

    /** GET {url}: todos los usuarios del AD. */
    public List<UsuarioAdDto> listarUsuarios() {
        return consultar(null);
    }

    /** GET {url}?codUsuario={valor}: búsqueda de usuario. */
    public List<UsuarioAdDto> buscarPorCodUsuario(String valor) {
        // Solo caracteres seguros: nada que permita colar otros parámetros en la URL del AD
        if (valor == null || !valor.matches("[A-Za-z0-9._-]{1,50}")) {
            throw new IllegalArgumentException("Valor de búsqueda inválido");
        }
        return consultar(valor);
    }

    private List<UsuarioAdDto> consultar(String codUsuario) {
        String url = propiedades.urlBase();
        if (url == null || url.isBlank()) {
            throw new ServicioExternoException("El servicio del AD no está configurado (ad.url-base)");
        }
        URI uri = UriComponentsBuilder.fromUriString(url.trim())
                .queryParamIfPresent(PARAMETRO_BUSQUEDA, Optional.ofNullable(codUsuario))
                .build().encode().toUri();

        RespuestaAdDto respuesta;
        try {
            respuesta = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(cabeceras -> {
                        if (propiedades.token() != null && !propiedades.token().isBlank()) {
                            cabeceras.setBearerAuth(propiedades.token());
                        }
                    })
                    .retrieve()
                    .body(RespuestaAdDto.class);
        } catch (RestClientResponseException e) {
            log.warn("El AD respondió HTTP {}", e.getStatusCode().value());
            throw new ServicioExternoException(mensajeParaEstado(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            log.warn("No se pudo comunicar con el AD ({})", e.getMostSpecificCause().getClass().getName());
            throw new ServicioExternoException(mensajeParaFalloDeRed(e));
        } catch (RestClientException e) {
            log.warn("Respuesta ilegible del AD ({})", e.getMostSpecificCause().getClass().getName());
            throw new ServicioExternoException("La respuesta del AD no tiene el formato esperado");
        }

        if (respuesta == null || respuesta.content() == null) {
            throw new ServicioExternoException("El AD respondió sin la lista de usuarios");
        }
        if (Boolean.FALSE.equals(respuesta.success())) {
            throw new ServicioExternoException("El AD reportó una respuesta no exitosa");
        }
        return respuesta.content();
    }

    private static String mensajeParaEstado(int codigo) {
        if (codigo == 401 || codigo == 403) {
            return "El AD rechazó las credenciales (HTTP " + codigo + ")";
        }
        if (codigo == 404) {
            return "La ruta del AD no existe (HTTP 404)";
        }
        if (codigo >= 500) {
            return "El AD respondió con un error interno (HTTP " + codigo + ")";
        }
        return "El AD rechazó la petición (HTTP " + codigo + ")";
    }

    private static String mensajeParaFalloDeRed(Throwable error) {
        if (tiene(error, SSLException.class)) {
            return mensajeParaFalloSsl(error);
        }
        if (tiene(error, HttpTimeoutException.class, SocketTimeoutException.class)) {
            return "El AD no respondió a tiempo";
        }
        if (tiene(error, ConnectException.class, UnknownHostException.class)) {
            return "No se pudo conectar con el AD";
        }
        return "Error de comunicación con el AD";
    }

    /** Distingue las tres causas habituales de un fallo TLS, sin exponer detalles internos. */
    private static String mensajeParaFalloSsl(Throwable error) {
        if (tiene(error, CertificateExpiredException.class)) {
            return "El certificado del AD está vencido";
        }
        if (tieneMensaje(error, "No subject alternative", "No name matching", "subject alternative names")) {
            return "El nombre del servidor del AD no coincide con su certificado";
        }
        if (tiene(error, CertPathBuilderException.class, CertPathValidatorException.class)
                || tieneMensaje(error, "PKIX path", "unable to find valid certification path")) {
            return "El certificado del AD no es confiable: su CA no está en el almacén de confianza";
        }
        return "No se pudo establecer una conexión segura con el AD";
    }

    private static boolean tieneMensaje(Throwable raiz, String... fragmentos) {
        for (Throwable t = raiz; t != null; t = t.getCause()) {
            String mensaje = t.getMessage();
            if (mensaje != null) {
                for (String fragmento : fragmentos) {
                    if (mensaje.contains(fragmento)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
}