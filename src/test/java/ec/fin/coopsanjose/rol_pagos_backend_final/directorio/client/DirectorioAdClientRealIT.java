package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client;

import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto.UsuarioAdDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

/** Contra el AD REAL. Se omite si no defines AD_TEST_USERNAME y AD_TEST_CEDULA en .env. */
@SpringBootTest
class DirectorioAdClientRealIT {

    @Autowired private DirectorioAdClient cliente;
    @Value("${AD_TEST_USERNAME:}") private String usuarioPrueba;
    @Value("${AD_TEST_CEDULA:}") private String cedulaPrueba;

    private void requerirDatosDePrueba() {
        assumeThat(usuarioPrueba).as("Defina AD_TEST_USERNAME en .env").isNotBlank();
        assumeThat(cedulaPrueba).as("Defina AD_TEST_CEDULA en .env").isNotBlank();
    }

    @Test
    void elListadoRealIncluyeAlUsuarioDePrueba() {
        requerirDatosDePrueba();

        List<UsuarioAdDto> todos = cliente.listarUsuarios();

        assertThat(todos).isNotEmpty();
        assertThat(todos).anySatisfy(u -> {
            assertThat(u.codigoUsuario()).isEqualToIgnoringCase(usuarioPrueba);
            assertThat(u.identificacion()).isEqualTo(cedulaPrueba);
        });
    }

    @Test
    void laBusquedaRealPorIdentificacionDevuelveAlUsuarioDePrueba() {
        requerirDatosDePrueba();

        List<UsuarioAdDto> encontrados = cliente.buscarPorCodUsuario(cedulaPrueba);

        assertThat(encontrados).anySatisfy(u ->
                assertThat(u.codigoUsuario()).isEqualToIgnoringCase(usuarioPrueba));
    }

    @Test
    void rechazaValoresPeligrososAntesDeLlamarAlAd() {
        assertThatThrownBy(() -> cliente.buscarPorCodUsuario("1712345675&admin=true"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cliente.buscarPorCodUsuario("a b"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}