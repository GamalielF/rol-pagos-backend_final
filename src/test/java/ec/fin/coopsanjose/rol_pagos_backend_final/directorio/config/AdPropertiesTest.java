package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdPropertiesTest {

    private static AdProperties conDominio(String dominio) {
        return new AdProperties("http://ad.test", "", null, Duration.ofSeconds(5), Duration.ofSeconds(30),
                dominio, List.of(), new AdProperties.Sincronizacion(false, "0 0 3 * * *", 50));
    }

    @Test
    void elCorreoEsElCodigoDeUsuarioMasElDominioInstitucional() {
        assertThat(conDominio("coopsanjose.fin.ec").correoDe("fsepa")).isEqualTo("fsepa@coopsanjose.fin.ec");
        assertThat(conDominio("coopsanjose.fin.ec").correoDe("juan.perez")).isEqualTo("juan.perez@coopsanjose.fin.ec");
    }

    @Test
    void elCodigoSeEscribeEnMinusculasYSinEspacios() {
        assertThat(conDominio("coopsanjose.fin.ec").correoDe("  FSepa ")).isEqualTo("fsepa@coopsanjose.fin.ec");
    }

    @Test
    void elDominioSeLimpiaDeEspacios() {
        assertThat(conDominio(" coopsanjose.fin.ec ").correoDe("fsepa")).isEqualTo("fsepa@coopsanjose.fin.ec");
    }

    @Test
    void sinCodigoDeUsuarioNoSeArmaUnaDireccionIncompleta() {
        assertThat(conDominio("coopsanjose.fin.ec").correoDe(null)).isNull();
        assertThat(conDominio("coopsanjose.fin.ec").correoDe("")).isNull();
        assertThat(conDominio("coopsanjose.fin.ec").correoDe("   ")).isNull();
    }

    @Test
    void sinDominioNoSeArmaUnaDireccionIncompleta() {
        assertThat(conDominio(null).correoDe("fsepa")).isNull();
        assertThat(conDominio("  ").correoDe("fsepa")).isNull();
    }
}
