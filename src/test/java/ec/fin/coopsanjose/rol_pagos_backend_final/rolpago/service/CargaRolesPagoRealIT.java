package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service.DirectorioAdService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.EstadoCarga;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoArchivoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/** Contra el AD REAL: el nombre del titular sale del AD en el momento de ejecutar, no de datos escritos aquí. */
class CargaRolesPagoRealIT extends SoporteRolesPago {

    @Autowired private DirectorioAdService directorioService;
    @Autowired private DirectorioAdClient clienteAd;
    @Value("${AD_TEST_USERNAME:}") private String usuarioPrueba;
    @Value("${AD_TEST_CEDULA:}") private String cedulaPrueba;

    @BeforeEach
    void requerirDatosDePrueba() {
        assumeThat(usuarioPrueba).as("Defina AD_TEST_USERNAME en .env").isNotBlank();
        assumeThat(cedulaPrueba).as("Defina AD_TEST_CEDULA en .env").isNotBlank();
    }

    private String nombreRealDelTitular() {
        return directorioRepository.findByUsernameAd(usuarioPrueba).orElseThrow().getNombreCompleto();
    }

    @Test
    void cargaElRolDelUsuarioDePruebaConSuNombreRealDelAd() {
        directorioService.sincronizar();
        em.flush();
        em.clear();
        byte[] pdf = rol(MES_PERIODO, nombreRealDelTitular());   // una sola generación

        ResultadoArchivoDto primera = cargarUno(cedulaPrueba + ".pdf", pdf);
        ResultadoArchivoDto segunda = cargarUno(cedulaPrueba + ".pdf", pdf);

        assertThat(primera.estado()).isEqualTo(EstadoCarga.CARGADO);
        assertThat(segunda.estado()).isEqualTo(EstadoCarga.SIN_CAMBIOS);
    }

    @Test
    void unNombreAjenoSeRechazaConLosDatosReales() {
        directorioService.sincronizar();
        em.flush();
        em.clear();

        ResultadoArchivoDto r = cargarUno(cedulaPrueba + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA));

        assertThat(r.estado()).isEqualTo(EstadoCarga.RECHAZADO);
        assertThat(r.mensaje()).contains("no coincide");
    }

    @Test
    void resuelveAlTitularConsultandoAlAdEnVivo() {
        // El directorio está vacío (lo deja así la base): el titular solo puede salir de la consulta en vivo
        String nombre = clienteAd.buscarPorCodUsuario(cedulaPrueba).stream()
                .filter(u -> usuarioPrueba.equalsIgnoreCase(u.codigoUsuario()))
                .findFirst().orElseThrow().nombre();

        ResultadoArchivoDto r = cargarUno(cedulaPrueba + ".pdf", rol(MES_PERIODO, nombre));

        assertThat(r.estado()).isEqualTo(EstadoCarga.CARGADO);
        assertThat(directorioRepository.findByUsernameAd(usuarioPrueba)).isPresent();
    }
}