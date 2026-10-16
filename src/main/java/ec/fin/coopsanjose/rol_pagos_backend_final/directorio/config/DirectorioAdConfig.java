package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(AdProperties.class)
public class DirectorioAdConfig {

    /** Con tiempos de espera: un AD lento nunca debe dejar colgada la aplicación. */
    @Bean
    public RestClient directorioAdRestClient(AdProperties propiedades, SslBundles sslBundles) {
        HttpClient.Builder http = HttpClient.newBuilder().connectTimeout(propiedades.timeoutConexion());
        if (propiedades.sslBundle() != null && !propiedades.sslBundle().isBlank()) {
            // Confía en la CA del AD SOLO para este cliente: el resto de la JVM no cambia
            http.sslContext(sslBundles.getBundle(propiedades.sslBundle()).createSslContext());
        }
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http.build());
        fabrica.setReadTimeout(propiedades.timeoutLectura());
        return RestClient.builder().requestFactory(fabrica).build();
    }
}