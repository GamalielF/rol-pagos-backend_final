package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreo;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreoApi;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreoSimulado;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;


import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties({CorreoProperties.class, NotificacionProperties.class})
public class NotificacionConfig {

    @Bean
    public EnviadorCorreo enviadorCorreo(CorreoProperties propiedades, SslBundles sslBundles, Environment entorno) {
        return switch (propiedades.modo()) {
            case API -> new EnviadorCorreoApi(propiedades.api(), crearRestClient(propiedades.api(), sslBundles),
                    EnvioCorreoProperties.desde(entorno));
            case SIMULADO -> {
                if (!entorno.matchesProfiles("dev")) {
                    // Un envío simulado en producción marcaría como "enviados" avisos que nunca salieron
                    throw new IllegalStateException("app.correo.modo=SIMULADO solo se permite en el perfil dev");
                }
                yield new EnviadorCorreoSimulado(propiedades.resultadoSimulado());
            }
        };
    }

    private static RestClient crearRestClient(CorreoProperties.Api api, SslBundles sslBundles) {
        HttpClient.Builder http = HttpClient.newBuilder().connectTimeout(api.timeoutConexion());
        if (api.sslBundle() != null && !api.sslBundle().isBlank()) {
            // Confía en la CA de la API de correo SOLO para esta conexión (igual que con el AD)
            http.sslContext(sslBundles.getBundle(api.sslBundle()).createSslContext());
        }
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http.build());
        fabrica.setReadTimeout(api.timeoutLectura());
        return RestClient.builder().requestFactory(fabrica).build();
    }
}
