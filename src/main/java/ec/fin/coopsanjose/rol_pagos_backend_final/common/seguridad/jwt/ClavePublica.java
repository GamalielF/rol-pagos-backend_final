package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Carga una clave pública RSA en PEM ("BEGIN PUBLIC KEY") o base64 pelado, igual que el JwtUtil del autenticador. */
public final class ClavePublica {

    private ClavePublica() {
    }

    public static RSAPublicKey cargar(String texto) {
        try {
            String limpio = texto.replaceAll("-----(BEGIN|END)[A-Z ]*-----", "").replaceAll("\\s+", "");
            PublicKey clave = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(limpio)));
            RSAPublicKey rsa = (RSAPublicKey) clave;
            if (rsa.getModulus().bitLength() < 2048) {
                throw new IllegalStateException("La clave RSA debe tener al menos 2048 bits");
            }
            return rsa;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "La clave pública JWT no es válida: se espera RSA en formato PEM X.509 o base64", e);
        }
    }
}