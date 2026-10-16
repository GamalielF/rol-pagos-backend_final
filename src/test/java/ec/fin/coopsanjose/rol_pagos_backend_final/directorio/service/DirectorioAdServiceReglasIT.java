package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ServicioExternoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto.UsuarioAdDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.ResultadoSincronizacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.SincronizacionAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.SincronizacionAdRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Reglas de seguridad de la sincronización. Usa datos de prueba porque el AD real no puede provocar estos casos. */
@SpringBootTest
@Transactional
class DirectorioAdServiceReglasIT {

    private static final List<UsuarioAdDto> CUATRO = List.of(
            ad("ua", "1712345675"), ad("ub", "1709876542"), ad("uc", "1723456784"), ad("ud", "1711111110"));

    @Autowired private DirectorioAdService servicio;
    @Autowired private DirectorioAdRepository repositorio;
    @Autowired private SincronizacionAdRepository bitacoras;
    @Autowired private EntityManager em;
    @MockitoBean private DirectorioAdClient cliente;

    @BeforeEach
    void limpiar() {
        em.createNativeQuery("DELETE FROM DIRECTORIO_AD").executeUpdate();   // se revierte al terminar
        em.flush();
        em.clear();
    }

    private static UsuarioAdDto ad(String usuario, String cedula) {
        return new UsuarioAdDto(usuario, "NOMBRE " + usuario.toUpperCase(), cedula, "CARGO", 1, "2023-04-14", 1);
    }

    private void sincronizarYLimpiarCache() {
        servicio.sincronizar();
        em.flush();
        em.clear();
    }

    @Test
    void ausenteSeInactivaYAlVolverSeReactiva() {
        when(cliente.listarUsuarios()).thenReturn(CUATRO);
        sincronizarYLimpiarCache();

        when(cliente.listarUsuarios()).thenReturn(CUATRO.subList(0, 3));
        assertThat(servicio.sincronizar().inactivados()).isEqualTo(1);
        em.flush();
        em.clear();
        assertThat(repositorio.findByUsernameAd("ud").orElseThrow().getEstadoRegistro())
                .isEqualTo(EstadoRegistro.INACTIVO);

        when(cliente.listarUsuarios()).thenReturn(CUATRO);
        assertThat(servicio.sincronizar().actualizados()).isEqualTo(1);
        em.flush();
        em.clear();
        assertThat(repositorio.findByUsernameAd("ud").orElseThrow().getEstadoRegistro())
                .isEqualTo(EstadoRegistro.ACTIVO);
    }

    @Test
    void unaListaVaciaFallaYNoTocaNada() {
        when(cliente.listarUsuarios()).thenReturn(CUATRO);
        sincronizarYLimpiarCache();

        when(cliente.listarUsuarios()).thenReturn(List.of());
        assertThatThrownBy(() -> servicio.sincronizar()).isInstanceOf(ServicioExternoException.class);

        em.flush();
        em.clear();
        assertThat(repositorio.countByEstadoRegistro(EstadoRegistro.ACTIVO)).isEqualTo(4);
        assertThat(bitacoras.findFirstByOrderByInicioDesc().orElseThrow().getResultado())
                .isEqualTo(ResultadoSincronizacion.FALLIDA);
    }

    @Test
    void unaRespuestaMuyPequenaSeRechazaYNoInactivaANadie() {
        when(cliente.listarUsuarios()).thenReturn(CUATRO);
        sincronizarYLimpiarCache();

        when(cliente.listarUsuarios()).thenReturn(CUATRO.subList(0, 1));   // 1 de 4: por debajo del 50 %
        assertThatThrownBy(() -> servicio.sincronizar())
                .isInstanceOf(ServicioExternoException.class)
                .hasMessageContaining("No se aplicó ningún cambio");

        em.flush();
        em.clear();
        assertThat(repositorio.countByEstadoRegistro(EstadoRegistro.ACTIVO)).isEqualTo(4);
    }

    @Test
    void cadaCorridaExitosaQuedaEnLaBitacora() {
        when(cliente.listarUsuarios()).thenReturn(CUATRO);
        sincronizarYLimpiarCache();

        SincronizacionAd ultima = bitacoras.findFirstByOrderByInicioDesc().orElseThrow();

        assertThat(ultima.getResultado()).isEqualTo(ResultadoSincronizacion.EXITOSA);
        assertThat(ultima.getRecibidos()).isEqualTo(4);
        assertThat(ultima.getCreados()).isEqualTo(4);
    }
}