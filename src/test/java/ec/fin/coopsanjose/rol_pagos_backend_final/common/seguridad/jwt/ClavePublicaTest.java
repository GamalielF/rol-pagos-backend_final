package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt;

import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClavePublicaTest {

    private static final TokensDePrueba CLAVES = TokensDePrueba.nuevo();

    @Test
    void cargaUnPemConEncabezados() {
        assertThat(ClavePublica.cargar(CLAVES.clavePublicaPem())).isEqualTo(CLAVES.publica());
    }

    @Test
    void cargaElBase64SinEncabezados() {
        String soloCuerpo = CLAVES.clavePublicaPem().replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s+", "");
        assertThat(ClavePublica.cargar(soloCuerpo)).isEqualTo(CLAVES.publica());
    }

    @Test
    void toleraSaltosDeLineaDeWindowsYEspaciosSobrantes() {
        String texto = "  " + CLAVES.clavePublicaPem().replace("\n", "\r\n") + "  ";
        assertThat(ClavePublica.cargar(texto)).isEqualTo(CLAVES.publica());
    }

    @Test
    void rechazaTextoQueNoEsUnaClave() {
        assertThatThrownBy(() -> ClavePublica.cargar("esto no es una clave"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no es válida");
    }

    @Test
    void rechazaUnaClaveRsaDeMenosDe2048Bits() {
        String pem = TokensDePrueba.pem(TokensDePrueba.generarRsa(1024).getPublic());
        assertThatThrownBy(() -> ClavePublica.cargar(pem))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("2048");
    }

    @Test
    void rechazaUnaClaveQueNoEsRsa() throws Exception {
        KeyPairGenerator curva = KeyPairGenerator.getInstance("EC");
        curva.initialize(256);
        String pem = TokensDePrueba.pem(curva.generateKeyPair().getPublic());
        assertThatThrownBy(() -> ClavePublica.cargar(pem))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no es válida");
    }

    @Test
    void unaClavePrivadaNoSeAceptaComoPublica() {
        byte[] privada = TokensDePrueba.generarRsa(2048).getPrivate().getEncoded();
        String pem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(privada)
                + "\n-----END PRIVATE KEY-----";
        assertThatThrownBy(() -> ClavePublica.cargar(pem)).isInstanceOf(IllegalStateException.class);
    }
}