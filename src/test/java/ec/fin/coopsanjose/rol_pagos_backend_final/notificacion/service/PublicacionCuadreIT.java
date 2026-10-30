package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.SoporteNotificaciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PublicacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResumenPeriodoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El cuadre de documentos y usuarios activos, en los dos sentidos. */
class PublicacionCuadreIT extends SoporteNotificaciones {

    @Autowired private AccionRolPagoRepository accionRepository;

    @BeforeEach
    void escenario() {
        personaConCorreo("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, LocalDate.of(2023, 1, 1));
        personaConCorreo("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, LocalDate.of(2023, 1, 1));
        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA))));
    }

    private void darDeBaja(String usuario) {
        DirectorioAd d = directorioRepository.findByUsernameAd(usuario).orElseThrow();
        d.inactivar();
        directorioRepository.save(d);
        em.flush();
    }

    @Test
    void siTodoCuadraNoSePideConfirmacion() {
        ResumenPeriodoDto resumen = publicacion.resumen(PERIODO);

        assertThat(resumen.rolesActivos()).isEqualTo(2);
        assertThat(resumen.personasActivas()).isEqualTo(2);
        assertThat(resumen.sinRol()).isZero();
        assertThat(resumen.rolesSinPersonaActiva()).isZero();
        assertThat(publicar().estadoPeriodo()).isEqualTo("PUBLICADO");
    }

    @Test
    void unRolCuyoTitularYaNoEstaActivoApareceEnElResumen() {
        darDeBaja("zz.juan");

        ResumenPeriodoDto resumen = publicacion.resumen(PERIODO);

        assertThat(resumen.rolesSinPersonaActiva()).isEqualTo(1);
        assertThat(resumen.nombresRolesSinPersona()).containsExactly(NOMBRE_JUAN);
        assertThat(resumen.personasActivas()).isEqualTo(1);
        assertThat(resumen.sinRol()).isZero();
    }

    @Test
    void publicarConUnTitularDadoDeBajaExigeConfirmacion() {
        darDeBaja("zz.juan");

        assertThatThrownBy(() -> publicar())
                .isInstanceOf(ConflictoException.class).hasMessageContaining("titular no está activo");
        assertThat(publicacion.resumen(PERIODO).estadoPeriodo()).isEqualTo("CARGADO");

        PublicacionDto resultado = publicacion.publicar(PERIODO, true, "zz.rrhh");
        assertThat(resultado.estadoPeriodo()).isEqualTo("PUBLICADO");
        assertThat(resultado.rolesSinPersonaActiva()).isEqualTo(1);
    }

    @Test
    void alTitularDadoDeBajaNoSeLeEncolaUnCorreo() {
        darDeBaja("zz.juan");

        PublicacionDto resultado = publicacion.publicar(PERIODO, true, "zz.rrhh");

        assertThat(resultado.avisosEncolados()).isEqualTo(1);   // solo María
        assertThat(avisoDe(CEDULA_JUAN).getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        assertThat(avisoDe(CEDULA_MARIA).getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
    }

    @Test
    void elEventoDePublicacionDejaConstanciaDeLosRolesSinTitular() {
        darDeBaja("zz.juan");

        publicacion.publicar(PERIODO, true, "zz.rrhh");

        em.flush();
        List<AccionRolPago> eventos = accionRepository.findAll().stream()
                .filter(a -> a.getTipoAccion() == TipoAccion.PERIODO_PUBLICADO && "2099-03".equals(a.getPeriodo())).toList();
        assertThat(eventos).singleElement().satisfies(a ->
                assertThat(a.getDetalle()).isEqualTo("roles=2, sinRol=0, rolesSinPersona=1, faltantes confirmados"));
    }
}
