package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.PerfilDirectorioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.ResultadoSincronizacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/** Sincroniza contra el AD REAL dentro de una transacción que se revierte: no toca tu directorio. */
@SpringBootTest
@Transactional
class DirectorioAdServiceRealIT {

    @Autowired private DirectorioAdService servicio;
    @Autowired private DirectorioAdRepository repositorio;
    @Autowired private EntityManager em;
    @Value("${AD_TEST_USERNAME:}") private String usuarioPrueba;
    @Value("${AD_TEST_CEDULA:}") private String cedulaPrueba;

    @BeforeEach
    void preparar() {
        assumeThat(usuarioPrueba).as("Defina AD_TEST_USERNAME en .env").isNotBlank();
        assumeThat(cedulaPrueba).as("Defina AD_TEST_CEDULA en .env").isNotBlank();
        em.createNativeQuery("DELETE FROM DIRECTORIO_AD").executeUpdate();   // se revierte al terminar
        em.flush();
        em.clear();
    }

    @Test
    void sincronizarContraElAdRealEsIdempotente() {
        ResultadoSincronizacionDto primera = servicio.sincronizar();
        em.flush();
        em.clear();
        ResultadoSincronizacionDto segunda = servicio.sincronizar();

        assertThat(primera.recibidos()).isPositive();
        assertThat(primera.creados()).isPositive();
        assertThat(segunda.creados()).isZero();
        assertThat(segunda.actualizados()).isZero();
        assertThat(segunda.inactivados()).isZero();
        assertThat(segunda.sinCambios()).isEqualTo(primera.creados());
    }

    @Test
    void elUsuarioDePruebaQuedaConSuCedulaYSuCorreo() {
        servicio.sincronizar();
        em.flush();
        em.clear();

        DirectorioAd d = repositorio.findByUsernameAd(usuarioPrueba).orElseThrow();

        assertThat(d.getCedula()).isEqualTo(cedulaPrueba);
        assertThat(d.getEmail()).isEqualTo(usuarioPrueba.toLowerCase() + "@coopsanjose.fin.ec");
        assertThat(d.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    @Test
    void buscarEnAdActualizaLaCopiaLocalSinInactivarANadie() {
        List<PerfilDirectorioDto> candidatos = servicio.buscarEnAd(cedulaPrueba);
        em.flush();
        em.clear();

        assertThat(candidatos).anySatisfy(c -> assertThat(c.username()).isEqualToIgnoringCase(usuarioPrueba));
        assertThat(repositorio.findByUsernameAd(usuarioPrueba)).isPresent();
    }
}