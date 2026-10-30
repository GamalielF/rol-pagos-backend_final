package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/** Un par de claves RSA de prueba y una fábrica de tokens con el formato del autenticador (y de sus imitaciones). */
public final class TokensDePrueba {

    private final RSAPrivateKey privada;
    private final RSAPublicKey publica;

    private TokensDePrueba(KeyPair par) {
        this.privada = (RSAPrivateKey) par.getPrivate();
        this.publica = (RSAPublicKey) par.getPublic();
    }

    public static TokensDePrueba nuevo() {
        return new TokensDePrueba(generarRsa(2048));
    }

    public static KeyPair generarRsa(int bits) {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(bits);
            return generador.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String pem(PublicKey clave) {
        String cuerpo = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(clave.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + cuerpo + "\n-----END PUBLIC KEY-----\n";
    }

    public RSAPublicKey publica() {
        return publica;
    }

    public String clavePublicaPem() {
        return pem(publica);
    }

    /** Las mismas propiedades que producción: sesión de 24 h más 5 minutos de margen. */
    public JwtProperties propiedades() {
        return new JwtProperties(clavePublicaPem(), null, Duration.parse("PT24H5M"), Duration.ofSeconds(60));
    }

    public static JWTClaimsSet.Builder reclamos(String usuario, Instant emitido, Instant vence) {
        return new JWTClaimsSet.Builder()
                .subject(usuario)
                .claim("username", usuario)
                .claim("userId", 1)
                .issueTime(Date.from(emitido))
                .expirationTime(Date.from(vence));
    }

    /** Una sesión normal de 30 minutos. */
    public static JWTClaimsSet.Builder sesion(String usuario) {
        Instant ahora = Instant.now();
        return reclamos(usuario, ahora, ahora.plus(Duration.ofMinutes(30)));
    }

    public String firmar(JWTClaimsSet.Builder reclamos) {
        return firmarCon(privada, reclamos);
    }

    public static String firmarConOtraClave(JWTClaimsSet.Builder reclamos) {
        return firmarCon((RSAPrivateKey) generarRsa(2048).getPrivate(), reclamos);
    }

    /** Un token sin firma (alg: none). */
    public static String sinFirma(JWTClaimsSet.Builder reclamos) {
        return new PlainJWT(reclamos.build()).serialize();
    }

    /** El ataque de confusión de algoritmo: HS256 usando la clave pública como secreto. */
    public static String hs256(JWTClaimsSet.Builder reclamos, byte[] secreto) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), reclamos.build());
            jwt.sign(new MACSigner(secreto));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String firmarCon(RSAPrivateKey clave, JWTClaimsSet.Builder reclamos) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), reclamos.build());
            jwt.sign(new RSASSASigner(clave));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}