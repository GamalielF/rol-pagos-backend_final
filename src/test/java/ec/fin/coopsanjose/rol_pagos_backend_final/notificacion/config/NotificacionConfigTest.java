package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config;

import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties.Modo;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config.CorreoProperties.ResultadoSimulado;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreo;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreoApi;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnviadorCorreoSimulado;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnvioCorreoException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NotificacionConfigTest {

    private final NotificacionConfig config = new NotificacionConfig();
    private final SslBundles sslBundles = mock(SslBundles.class);

    private static CorreoProperties propiedades(Modo modo, ResultadoSimulado resultado, String url) {
        return new CorreoProperties(modo, resultado, new CorreoProperties.Api(url, "token", "Authorization", "Bearer",
                null, Duration.ofSeconds(5), Duration.ofSeconds(15), null));
    }

    private static MockEnvironment entorno(String... perfiles) {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles(perfiles);
        return entorno;
    }

    @Test
    void elSimuladoSeCreaEnDev() {
        EnviadorCorreo enviador = config.enviadorCorreo(propiedades(Modo.SIMULADO, ResultadoSimulado.OK, null), sslBundles, entorno("dev"));

        assertThat(enviador).isInstanceOf(EnviadorCorreoSimulado.class);
        assertThat(enviador.estaConfigurado()).isTrue();
    }

    @Test
    void elSimuladoNoArrancaFueraDeDev() {
        CorreoProperties simulado = propiedades(Modo.SIMULADO, ResultadoSimulado.OK, null);

        assertThatThrownBy(() -> config.enviadorCorreo(simulado, sslBundles, entorno("prod")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("perfil dev");
        assertThatThrownBy(() -> config.enviadorCorreo(simulado, sslBundles, entorno()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elModoApiCreaElEnviadorDeLaApi() {
        assertThat(config.enviadorCorreo(propiedades(Modo.API, ResultadoSimulado.OK, null), sslBundles, entorno("prod")))
                .isInstanceOf(EnviadorCorreoApi.class);
    }

    @Test
    void laApiNoEstaConfiguradaHastaTenerUrl() {
        assertThat(config.enviadorCorreo(propiedades(Modo.API, ResultadoSimulado.OK, null), sslBundles, entorno("prod"))
                .estaConfigurado()).isFalse();
        assertThat(config.enviadorCorreo(propiedades(Modo.API, ResultadoSimulado.OK, "   "), sslBundles, entorno("prod"))
                .estaConfigurado()).isFalse();
        assertThat(config.enviadorCorreo(propiedades(Modo.API, ResultadoSimulado.OK, "https://correo.interno.test/enviar"),
                sslBundles, entorno("prod")).estaConfigurado()).isTrue();
    }

    @Test
    void elSimuladoSimulaFallosTemporalesYDefinitivos() {
        EnviadorCorreo ok = config.enviadorCorreo(propiedades(Modo.SIMULADO, ResultadoSimulado.OK, null), sslBundles, entorno("dev"));
        EnviadorCorreo temporal = config.enviadorCorreo(propiedades(Modo.SIMULADO, ResultadoSimulado.FALLO_TEMPORAL, null), sslBundles, entorno("dev"));
        EnviadorCorreo definitivo = config.enviadorCorreo(propiedades(Modo.SIMULADO, ResultadoSimulado.FALLO_DEFINITIVO, null), sslBundles, entorno("dev"));

        assertThatCode(() -> ok.enviar("a@b.test", "asunto", "texto", "aviso-1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> temporal.enviar("a@b.test", "asunto", "texto", "aviso-1"))
                .isInstanceOfSatisfying(EnvioCorreoException.class, e -> assertThat(e.esReintentable()).isTrue());
        assertThatThrownBy(() -> definitivo.enviar("a@b.test", "asunto", "texto", "aviso-1"))
                .isInstanceOfSatisfying(EnvioCorreoException.class, e -> assertThat(e.esReintentable()).isFalse());
    }
}
