package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.SoporteNotificaciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnvioCorreoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ResultadoReenvioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoResumenDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.GestionRolesPagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Reenviar a mano el aviso de un rol ya publicado. */
class ReenvioNotificacionServiceIT extends SoporteNotificaciones {

    @Autowired private ReenvioNotificacionService reenvio;
    @Autowired private GestionRolesPagoService gestion;
    @Autowired private AccionRolPagoRepository accionRepository;

    @BeforeEach
    void escenario() {
        prepararTresPersonas();
    }

    private ResultadoReenvioDto reenviar(String cedula) {
        return reenvio.reenviar(PERIODO, rolDe(cedula).getId());
    }

    /** Publica y deja los tres avisos enviados, como pasa con el despachador. */
    private void publicarYEnviarTodo() {
        publicar();
        despacho.despacharLote();
    }

    private long reenviosRegistrados() {
        em.flush();
        return accionRepository.findAll().stream()
                .filter(a -> a.getTipoAccion() == TipoAccion.NOTIFICACION_REENVIADA && "2099-03".equals(a.getPeriodo())).count();
    }

    @Test
    void reenviarUnAvisoYaEnviadoLoDejaEnviadoYAnotaElReenvio() {
        publicarYEnviarTodo();
        LocalDateTime enviadaOriginal = avisoDe(CEDULA_JUAN).getEnviadaEn();
        Long idAviso = avisoDe(CEDULA_JUAN).getId();

        ResultadoReenvioDto resultado = reenviar(CEDULA_JUAN);

        assertThat(resultado.aceptado()).isTrue();
        verify(enviador).reenviar(eq("zz.juan@coopsanjose.fin.ec"), anyString(), anyString(),
                eq("reenvio-" + idAviso + "-1"), anyMap());
        NotificacionRolPago aviso = avisoDe(CEDULA_JUAN);
        assertThat(aviso.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(aviso.getReenvios()).isEqualTo(1);
        assertThat(aviso.getReenviadaEn()).isNotNull();
        assertThat(aviso.getEnviadaEn()).isEqualTo(enviadaOriginal);
        assertThat(resultado.notificacion().reenvios()).isEqualTo(1);
        assertThat(resultado.notificacion().reenviadaEn()).isNotNull();
    }

    @Test
    void cadaReenvioSumaYUsaUnaReferenciaDistintaParaQueLaApiNoLosTomeComoDuplicados() {
        publicarYEnviarTodo();
        Long idAviso = avisoDe(CEDULA_JUAN).getId();

        reenviar(CEDULA_JUAN);
        reenviar(CEDULA_JUAN);

        assertThat(avisoDe(CEDULA_JUAN).getReenvios()).isEqualTo(2);
        verify(enviador).reenviar(any(), any(), any(), eq("reenvio-" + idAviso + "-1"), any());
        verify(enviador).reenviar(any(), any(), any(), eq("reenvio-" + idAviso + "-2"), any());
    }

    @Test
    void unAvisoFallidoSeReenviaYQuedaEnviadoSinElErrorAnterior() {
        publicar();
        doThrow(new EnvioCorreoException("La API de correo rechazó la solicitud (HTTP 422)", false, null))
                .when(enviador).enviar(eq("zz.maria@coopsanjose.fin.ec"), any(), any(), any());
        despacho.despacharLote();
        assertThat(avisoDe(CEDULA_MARIA).getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);

        ResultadoReenvioDto resultado = reenviar(CEDULA_MARIA);

        assertThat(resultado.aceptado()).isTrue();
        NotificacionRolPago aviso = avisoDe(CEDULA_MARIA);
        assertThat(aviso.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(aviso.getUltimoError()).isNull();
        assertThat(aviso.getEnviadaEn()).isNotNull();
        assertThat(aviso.getReenvios()).isEqualTo(1);
    }

    @Test
    void siElReenvioFallaElAvisoQuedaFallidoConElMotivoYLaFechaDelIntento() {
        publicarYEnviarTodo();
        LocalDateTime enviadaOriginal = avisoDe(CEDULA_JUAN).getEnviadaEn();
        doThrow(new EnvioCorreoException("La API de correo no respondió a tiempo", true, null))
                .when(enviador).reenviar(any(), any(), any(), any(), any());

        ResultadoReenvioDto resultado = reenviar(CEDULA_JUAN);

        assertThat(resultado.aceptado()).isFalse();
        assertThat(resultado.mensaje()).isEqualTo("La API de correo no respondió a tiempo");
        NotificacionRolPago aviso = avisoDe(CEDULA_JUAN);
        assertThat(aviso.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(aviso.getUltimoError()).isEqualTo("La API de correo no respondió a tiempo");
        assertThat(aviso.getReenviadaEn()).isNotNull();
        assertThat(aviso.getReenvios()).isEqualTo(1);
        assertThat(aviso.getEnviadaEn()).isEqualTo(enviadaOriginal);
    }

    @Test
    void unErrorInesperadoTambienQuedaAnotadoEnElAviso() {
        publicarYEnviarTodo();
        doThrow(new IllegalStateException("fallo raro")).when(enviador).reenviar(any(), any(), any(), any(), any());

        ResultadoReenvioDto resultado = reenviar(CEDULA_JUAN);

        assertThat(resultado.aceptado()).isFalse();
        assertThat(avisoDe(CEDULA_JUAN).getUltimoError()).contains("IllegalStateException").doesNotContain("fallo raro");
    }

    @Test
    void unRolSinAvisoRecibeUnoNuevoAlReenviar() {
        publicarYEnviarTodo();
        // Los repositorios no tienen delete (BaseRepository): se quita con SQL nativo, dentro de la transacción de la prueba
        em.createNativeQuery("DELETE FROM NOTIFICACIONES_ROLES_PAGO WHERE ID = ?1")
                .setParameter(1, avisoDe(CEDULA_LUIS).getId()).executeUpdate();
        em.flush();
        em.clear();

        ResultadoReenvioDto resultado = reenviar(CEDULA_LUIS);

        assertThat(resultado.aceptado()).isTrue();
        assertThat(avisos()).hasSize(3);
        NotificacionRolPago nuevo = avisoDe(CEDULA_LUIS);
        assertThat(nuevo.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(nuevo.getEmailDestino()).isEqualTo("zz.luis@coopsanjose.fin.ec");
        assertThat(nuevo.getEnviadaEn()).isNotNull();
        assertThat(nuevo.getReenvios()).isEqualTo(1);
    }

    @Test
    void elCorreoSeArmaConElCodigoDeUsuarioAunqueElDirectorioGuardeOtroDato() {
        publicarYEnviarTodo();
        DirectorioAd juan = directorioRepository.findByUsernameAd("zz.juan").orElseThrow();
        juan.setEmail("dato.viejo@otro-dominio.com");
        directorioRepository.save(juan);
        em.flush();

        reenviar(CEDULA_JUAN);

        verify(enviador).reenviar(eq("zz.juan@coopsanjose.fin.ec"), any(), any(), any(), any());
        assertThat(avisoDe(CEDULA_JUAN).getEmailDestino()).isEqualTo("zz.juan@coopsanjose.fin.ec");
    }

    @Test
    void unAvisoQueEstaEnColaNoSePuedeReenviarYNoSeEnviaNada() {
        publicar();

        assertThatThrownBy(() -> reenviar(CEDULA_JUAN))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("en cola de envío");
        verify(enviador, never()).reenviar(any(), any(), any(), any(), any());
    }

    @Test
    void unPeriodoSinPublicarNoTieneAvisosQueReenviar() {
        assertThatThrownBy(() -> reenviar(CEDULA_JUAN))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("aún no está publicado");
    }

    @Test
    void unRolQueNoEsDeEstePeriodoNoSeEncuentra() {
        publicarYEnviarTodo();

        assertThatThrownBy(() -> reenvio.reenviar(PERIODO, 999_999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void sinCuentaActivaEnElDirectorioNoSePuedeArmarElCorreo() {
        publicarYEnviarTodo();
        DirectorioAd juan = directorioRepository.findByUsernameAd("zz.juan").orElseThrow();
        juan.inactivar();
        directorioRepository.save(juan);
        em.flush();

        assertThatThrownBy(() -> reenviar(CEDULA_JUAN))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("cuenta activa");
        verify(enviador, never()).reenviar(any(), any(), any(), any(), any());
    }

    @Test
    void unRolInactivoNoTieneAvisoQueReenviar() {
        gestion.inactivar(PERIODO, rolDe(CEDULA_JUAN).getId());
        publicacion.publicar(PERIODO, true, "zz.rrhh");
        despacho.despacharLote();

        assertThatThrownBy(() -> reenviar(CEDULA_JUAN))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("inactivo");
    }

    @Test
    void cadaReenvioQuedaEnLaBitacora() {
        publicarYEnviarTodo();

        reenviar(CEDULA_JUAN);
        reenviar(CEDULA_MARIA);

        assertThat(reenviosRegistrados()).isEqualTo(2);
    }

    @Test
    void elListadoDeRolesMuestraElReenvio() {
        publicarYEnviarTodo();
        reenviar(CEDULA_JUAN);

        RolPagoResumenDto fila = gestion.listar(PERIODO).roles().stream()
                .filter(f -> CEDULA_JUAN.equals(f.cedula())).findFirst().orElseThrow();
        RolPagoResumenDto otra = gestion.listar(PERIODO).roles().stream()
                .filter(f -> CEDULA_MARIA.equals(f.cedula())).findFirst().orElseThrow();

        assertThat(fila.notificacion().estado()).isEqualTo("ENVIADA");
        assertThat(fila.notificacion().reenvios()).isEqualTo(1);
        assertThat(fila.notificacion().reenviadaEn()).isNotNull();
        assertThat(otra.notificacion().reenvios()).isZero();
        assertThat(otra.notificacion().reenviadaEn()).isNull();
    }
}
