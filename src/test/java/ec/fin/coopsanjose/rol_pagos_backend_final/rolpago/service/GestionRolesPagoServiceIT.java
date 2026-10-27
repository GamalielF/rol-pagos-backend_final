package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ListadoPeriodoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoResumenDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.repository.NotificacionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.NotificacionRolDto;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionRolesPagoServiceIT extends SoporteRolesPago {

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private GestionRolesPagoService gestion;
    @Autowired private NotificacionRolPagoRepository avisos;

    @BeforeEach
    void unRolCargado() {
        persona("jperez", CEDULA_JUAN, NOMBRE_JUAN, true);
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
    }

    @Test
    void listarMuestraElNombreYLaCedulaCompleta() {
        ListadoPeriodoDto listado = gestion.listar(PERIODO);

        assertThat(listado.periodo()).isEqualTo("2099-03");
        assertThat(listado.estadoPeriodo()).isEqualTo("CARGADO");
        assertThat(listado.activos()).isEqualTo(1);
        assertThat(listado.roles()).hasSize(1);
        RolPagoResumenDto fila = listado.roles().get(0);
        assertThat(fila.cedula()).isEqualTo(CEDULA_JUAN);
        assertThat(fila.nombre()).isEqualTo(NOMBRE_JUAN);
        assertThat(listado.toString()).doesNotContain(CEDULA_JUAN);
    }

    @Test
    void inactivarYActivarCambianElEstado() {
        Long id = rolDe(CEDULA_JUAN).getId();

        assertThat(gestion.inactivar(PERIODO, id).estado()).isEqualTo("INACTIVO");
        assertThat(gestion.listar(PERIODO).inactivos()).isEqualTo(1);
        assertThat(gestion.activar(PERIODO, id).estado()).isEqualTo("ACTIVO");
    }

    @Test
    void unRolNoSePuedeTocarDesdeOtroPeriodo() {
        Long id = rolDe(CEDULA_JUAN).getId();

        assertThatThrownBy(() -> gestion.inactivar(YearMonth.of(2099, 4), id))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void unPeriodoPublicadoNoSeModifica() {
        Long id = rolDe(CEDULA_JUAN).getId();
        Periodo periodo = periodoRepository.findByFechaPeriodo(PERIODO.atDay(1)).orElseThrow();
        periodo.setEstadoPeriodo(EstadoPeriodo.PUBLICADO);
        periodoRepository.save(periodo);
        em.flush();

        assertThatThrownBy(() -> gestion.inactivar(PERIODO, id)).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> gestion.activar(PERIODO, id)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void listarUnPeriodoQueNoExisteDa404() {
        assertThatThrownBy(() -> gestion.listar(YearMonth.of(2098, 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private NotificacionRolPago avisoDe(String cedula, EstadoNotificacion estado) {
        NotificacionRolPago aviso = new NotificacionRolPago();
        aviso.setPeriodo(periodoRepository.findByFechaPeriodo(PERIODO.atDay(1)).orElseThrow());
        aviso.setRolPagoId(rolDe(cedula).getId());
        aviso.setCedula(cedula);
        aviso.setUsernameDestino("jperez");
        aviso.setEmailDestino("jperez@coopsanjose.fin.ec");
        aviso.setEstado(estado);
        return aviso;
    }

    private RolPagoResumenDto filaDe(String cedula) {
        return gestion.listar(PERIODO).roles().stream().filter(f -> cedula.equals(f.cedula())).findFirst().orElseThrow();
    }

    @Test
    void unRolSinAvisoMuestraLaNotificacionVacia() {
        assertThat(filaDe(CEDULA_JUAN).notificacion()).isNull();
    }

    @Test
    void elListadoTraeElEstadoDelAvisoDeCadaRolYNoMezclaLosDeOtros() {
        persona("mgomez", CEDULA_MARIA, NOMBRE_MARIA, true);
        cargarUno(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA));
        NotificacionRolPago enviado = avisoDe(CEDULA_JUAN, EstadoNotificacion.ENVIADA);
        enviado.setIntentos(1);
        enviado.setEnviadaEn(LocalDateTime.of(2099, 3, 2, 10, 15));
        avisos.save(enviado);

        NotificacionRolDto deJuan = filaDe(CEDULA_JUAN).notificacion();
        assertThat(deJuan).isNotNull();
        assertThat(deJuan.estado()).isEqualTo("ENVIADA");
        assertThat(deJuan.intentos()).isEqualTo(1);
        assertThat(deJuan.enviadaEn()).isEqualTo(LocalDateTime.of(2099, 3, 2, 10, 15));
        assertThat(filaDe(CEDULA_MARIA).notificacion()).isNull();
    }

    @Test
    void unAvisoFallidoTraeElMotivoDelError() {
        NotificacionRolPago fallido = avisoDe(CEDULA_JUAN, EstadoNotificacion.FALLIDA);
        fallido.setIntentos(5);
        fallido.setUltimoError("La API de correo no respondio a tiempo");
        avisos.save(fallido);

        NotificacionRolDto notificacion = filaDe(CEDULA_JUAN).notificacion();
        assertThat(notificacion.estado()).isEqualTo("FALLIDA");
        assertThat(notificacion.intentos()).isEqualTo(5);
        assertThat(notificacion.ultimoError()).isEqualTo("La API de correo no respondio a tiempo");
        assertThat(notificacion.enviadaEn()).isNull();
    }

    @Test
    void unAvisoPendienteEnReintentoTraeCuandoSeVolveraAIntentar() {
        NotificacionRolPago pendiente = avisoDe(CEDULA_JUAN, EstadoNotificacion.PENDIENTE);
        pendiente.setIntentos(2);
        pendiente.setProximoIntento(LocalDateTime.of(2099, 3, 2, 10, 20));
        avisos.save(pendiente);

        NotificacionRolDto notificacion = filaDe(CEDULA_JUAN).notificacion();
        assertThat(notificacion.estado()).isEqualTo("PENDIENTE");
        assertThat(notificacion.proximoIntento()).isEqualTo(LocalDateTime.of(2099, 3, 2, 10, 20));
    }

    @Test
    void laRespuestaDeInactivarNoTraeNotificacion() {
        assertThat(gestion.inactivar(PERIODO, rolDe(CEDULA_JUAN).getId()).notificacion()).isNull();
    }
}
