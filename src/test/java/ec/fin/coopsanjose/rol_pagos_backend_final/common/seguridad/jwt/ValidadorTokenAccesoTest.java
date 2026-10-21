package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorTokenAccesoTest {

    private static final TokensDePrueba CLAVES = TokensDePrueba.nuevo();

    private final ValidadorTokenAcceso validador = new ValidadorTokenAcceso(CLAVES.propiedades());

    private static Instant ahora() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static JWTClaimsSet.Builder conVida(Duration vida) {
        return TokensDePrueba.reclamos("fsepa", ahora(), ahora().plus(vida));
    }

    private void assertRechazado(String token, String fragmento) {
        assertThatThrownBy(() -> validador.validar(token))
                .isInstanceOfSatisfying(TokenInvalidoException.class,
                        e -> assertThat(e.categoria()).containsIgnoringCase(fragmento));
    }

    // ------------------------------------------------------------ lo que SÍ se acepta

    @Test
    void unaSesionValidaDevuelveElCodigoDeUsuario() {
        assertThat(validador.validar(CLAVES.firmar(TokensDePrueba.sesion("fsepa")))).isEqualTo("fsepa");
    }

    @Test
    void elCodigoDeUsuarioSeNormalizaAMinusculas() {
        assertThat(validador.validar(CLAVES.firmar(TokensDePrueba.sesion("FSEPA")))).isEqualTo("fsepa");
    }

    @Test
    void aceptaTokensConTypeAccess() {
        assertThat(validador.validar(CLAVES.firmar(TokensDePrueba.sesion("fsepa").claim("type", "ACCESS"))))
                .isEqualTo("fsepa");
    }

    @Test
    void aceptaUnaSesionDeExactamente24Horas() {
        assertThat(validador.validar(CLAVES.firmar(conVida(Duration.ofHours(24))))).isEqualTo("fsepa");
    }

    @Test
    void aceptaUnaSesionHastaElMargenDe5Minutos() {
        assertThat(validador.validar(CLAVES.firmar(conVida(Duration.parse("PT24H5M"))))).isEqualTo("fsepa");
    }

    @Test
    void toleraUnPocoDeDesfaseDeRelojEnElVencimiento() {
        Instant ahora = ahora();
        JWTClaimsSet.Builder reciencVencido = TokensDePrueba.reclamos("fsepa", ahora.minus(Duration.ofMinutes(31)),
                ahora.minus(Duration.ofSeconds(30)));
        assertThat(validador.validar(CLAVES.firmar(reciencVencido))).isEqualTo("fsepa");
    }

    @Test
    void elUsernameIgualAlSubSeAceptaSinImportarLasMayusculas() {
        assertThat(validador.validar(CLAVES.firmar(TokensDePrueba.sesion("fsepa").claim("username", "FSEPA"))))
                .isEqualTo("fsepa");
    }

    // ------------------------------------------------------------ tipo y vida

    @Test
    void rechazaElTokenTemporalDelPasoMfa() {
        assertRechazado(CLAVES.firmar(TokensDePrueba.sesion("fsepa").claim("type", "MFA_CHALLENGE")), "tipo de token");
    }

    @Test
    void rechazaUnTokenDeTipoRefresh() {
        assertRechazado(CLAVES.firmar(TokensDePrueba.sesion("fsepa").claim("type", "REFRESH")), "tipo de token");
    }

    @Test
    void rechazaUnTokenConVidaDeRenovacionDe7Dias() {
        assertRechazado(CLAVES.firmar(conVida(Duration.ofDays(7))), "vida del token");
    }

    @Test
    void rechazaUnaSesionMasLargaQueElMaximo() {
        assertRechazado(CLAVES.firmar(conVida(Duration.parse("PT24H6M"))), "vida del token");
    }

    @Test
    void rechazaUnTokenVencido() {
        Instant ahora = ahora();
        assertRechazado(CLAVES.firmar(TokensDePrueba.reclamos("fsepa", ahora.minus(Duration.ofHours(2)),
                ahora.minus(Duration.ofHours(1)))), "expired");
    }

    @Test
    void rechazaUnTokenQueTodaviaNoEsValido() {
        Date dentroDe10Minutos = Date.from(ahora().plus(Duration.ofMinutes(10)));
        assertRechazado(CLAVES.firmar(TokensDePrueba.sesion("fsepa").notBeforeTime(dentroDe10Minutos)), "before");
    }

    @Test
    void rechazaUnTokenSinFechaDeEmisionAunqueSuVencimientoSeaLejano() {
        JWTClaimsSet.Builder sinIatYDe7Dias = new JWTClaimsSet.Builder().subject("fsepa").claim("username", "fsepa")
                .expirationTime(Date.from(ahora().plus(Duration.ofDays(7))));
        assertRechazado(CLAVES.firmar(sinIatYDe7Dias), "iat");
    }

    @Test
    void rechazaUnTokenSinVencimiento() {
        JWTClaimsSet.Builder sinExp = new JWTClaimsSet.Builder().subject("fsepa").issueTime(Date.from(ahora()));
        assertRechazado(CLAVES.firmar(sinExp), "iat y exp");
    }

    // ------------------------------------------------------------ firma y algoritmo

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        assertRechazado(TokensDePrueba.firmarConOtraClave(TokensDePrueba.sesion("fsepa")), "firma");
    }

    @Test
    void rechazaUnTokenSinFirmaConAlgNone() {
        assertRechazado(TokensDePrueba.sinFirma(TokensDePrueba.sesion("fsepa")), "firma");
    }

    @Test
    void rechazaLaConfusionDeAlgoritmoHs256ConLaClavePublicaComoSecreto() {
        assertRechazado(TokensDePrueba.hs256(TokensDePrueba.sesion("fsepa"), CLAVES.publica().getEncoded()), "firma");
    }

    @Test
    void rechazaUnTokenAlteradoDespuesDeFirmado() {
        String token = CLAVES.firmar(TokensDePrueba.sesion("fsepa"));
        String[] partes = token.split("\\.");
        String otroContenido = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin\",\"username\":\"admin\",\"iat\":1,\"exp\":99999999999}".getBytes());
        assertRechazado(partes[0] + "." + otroContenido + "." + partes[2], "firma");
    }

    @Test
    void rechazaBasuraQueNoEsUnToken() {
        assertRechazado("abc.def.ghi", "firma");
        assertRechazado("no-es-un-token", "firma");
    }

    // ------------------------------------------------------------ contenido

    @Test
    void rechazaUnSubConCaracteresInvalidos() {
        assertRechazado(CLAVES.firmar(TokensDePrueba.sesion("fsepa; drop table")), "sub invalido");
    }

    @Test
    void rechazaUnTokenSinSub() {
        Instant ahora = ahora();
        JWTClaimsSet.Builder sinSub = new JWTClaimsSet.Builder().issueTime(Date.from(ahora))
                .expirationTime(Date.from(ahora.plus(Duration.ofMinutes(30))));
        assertRechazado(CLAVES.firmar(sinSub), "sub invalido");
    }

    @Test
    void rechazaUnUsernameDistintoDelSub() {
        assertRechazado(CLAVES.firmar(TokensDePrueba.sesion("fsepa").claim("username", "otro")), "no coinciden");
    }

    @Test
    void rechazaTokensVaciosNulosOExageradamenteLargos() {
        assertRechazado(null, "formato");
        assertRechazado("   ", "formato");
        assertRechazado("a".repeat(8193), "formato");
    }

    // ------------------------------------------------------------ configuración

    @Test
    void sinClaveConfiguradaNoValidaNada() {
        ValidadorTokenAcceso sinClave = new ValidadorTokenAcceso(
                new JwtProperties(null, null, Duration.ofHours(24), Duration.ofSeconds(60)));
        assertThat(sinClave.estaConfigurado()).isFalse();
        assertThatThrownBy(() -> sinClave.validar(CLAVES.firmar(TokensDePrueba.sesion("fsepa"))))
                .isInstanceOfSatisfying(TokenInvalidoException.class,
                        e -> assertThat(e.categoria()).isEqualTo("sin_clave_configurada"));
    }

    @Test
    void unaClaveInvalidaImpideArrancar() {
        JwtProperties propiedades = new JwtProperties("no es una clave", null, Duration.ofHours(24), Duration.ofSeconds(60));
        assertThatThrownBy(() -> new ValidadorTokenAcceso(propiedades)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void leeLaClaveDeUnArchivo(@TempDir Path carpeta) throws IOException {
        Path archivo = Files.writeString(carpeta.resolve("jwt-public.pem"), CLAVES.clavePublicaPem());
        ValidadorTokenAcceso conArchivo = new ValidadorTokenAcceso(
                new JwtProperties("", archivo.toString(), Duration.ofHours(24), Duration.ofSeconds(60)));
        assertThat(conArchivo.estaConfigurado()).isTrue();
        assertThat(conArchivo.validar(CLAVES.firmar(TokensDePrueba.sesion("fsepa")))).isEqualTo("fsepa");
    }

    @Test
    void unArchivoInexistenteDejaLaClaveSinConfigurar(@TempDir Path carpeta) {
        ValidadorTokenAcceso sinArchivo = new ValidadorTokenAcceso(new JwtProperties(
                "", carpeta.resolve("no-existe.pem").toString(), Duration.ofHours(24), Duration.ofSeconds(60)));
        assertThat(sinArchivo.estaConfigurado()).isFalse();
    }

    @Test
    void laClaveDeLaPropiedadTienePrioridadSobreLaDelArchivo(@TempDir Path carpeta) throws IOException {
        Path archivo = Files.writeString(carpeta.resolve("otra.pem"),
                TokensDePrueba.pem(TokensDePrueba.generarRsa(2048).getPublic()));
        ValidadorTokenAcceso ambas = new ValidadorTokenAcceso(new JwtProperties(
                CLAVES.clavePublicaPem(), archivo.toString(), Duration.ofHours(24), Duration.ofSeconds(60)));
        assertThat(ambas.validar(CLAVES.firmar(TokensDePrueba.sesion("fsepa")))).isEqualTo("fsepa");
    }
}