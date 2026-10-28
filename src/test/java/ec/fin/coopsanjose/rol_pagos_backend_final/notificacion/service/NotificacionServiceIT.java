package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.SoporteNotificaciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.correo.EnvioCorreoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.InformeNotificacionesDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ProblemaNotificacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response.ResultadoEncoladoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.PublicacionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class NotificacionServiceIT extends SoporteNotificaciones {

    @Autowired private AccionRolPagoRepository accionRepository;

    private long acciones(TipoAccion tipo) {
        em.flush();
        return accionRepository.findAll().stream()
                .filter(a -> a.getTipoAccion() == tipo && "2099-03".equals(a.getPeriodo())).count();
    }

    /** Juan recibe su aviso, a María el servicio le rechaza la dirección, y a Luis no se le puede armar el correo (sin código de usuario). */
    private void escenarioConUnFallidoYUnOmitido() {
        prepararTresPersonas();
        DirectorioAd luis = directorioRepository.findByUsernameAd("zz.luis").orElseThrow();
        luis.setUsernameAd(" ");
        directorioRepository.save(luis);
        em.flush();
        publicar();
        doThrow(new EnvioCorreoException("La API de correo rechazó la solicitud (HTTP 422)", false, null))
                .when(enviador).enviar(eq("zz.maria@coopsanjose.fin.ec"), anyString(), anyString(), anyString());
        despacho.despacharLote();
    }

    @Test
    void publicarEncolaUnAvisoPorPersona() {
        prepararTresPersonas();

        PublicacionDto resultado = publicar();

        assertThat(resultado.avisosEncolados()).isEqualTo(3);
        assertThat(avisos()).hasSize(3).allSatisfy(a -> {
            assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
            assertThat(a.getIntentos()).isZero();
            assertThat(a.getUltimoError()).isNull();
        });
        assertThat(avisoDe(CEDULA_JUAN).getUsernameDestino()).isEqualTo("zz.juan");
        assertThat(avisoDe(CEDULA_JUAN).getEmailDestino()).isEqualTo("zz.juan@coopsanjose.fin.ec");
    }

    @Test
    void elCorreoDelAvisoSeArmaConElCodigoDeUsuarioAunqueElDirectorioGuardeOtroDato() {
        prepararTresPersonas();
        DirectorioAd juan = directorioRepository.findByUsernameAd("zz.juan").orElseThrow();
        juan.setEmail("dato.viejo@otro-dominio.com");
        DirectorioAd luis = directorioRepository.findByUsernameAd("zz.luis").orElseThrow();
        luis.setEmail(null);
        directorioRepository.save(juan);
        directorioRepository.save(luis);
        em.flush();

        publicar();

        assertThat(avisoDe(CEDULA_JUAN).getEmailDestino()).isEqualTo("zz.juan@coopsanjose.fin.ec");
        assertThat(avisoDe(CEDULA_LUIS).getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(avisoDe(CEDULA_LUIS).getEmailDestino()).isEqualTo("zz.luis@coopsanjose.fin.ec");
    }

    @Test
    void conVariasCuentasSeAvisaUnaSolaVezALaMasReciente() {
        personaConCorreo("zz.juan.antiguo", CEDULA_JUAN, NOMBRE_JUAN, LocalDate.of(2020, 1, 1));
        personaConCorreo("zz.juan.reciente", CEDULA_JUAN, NOMBRE_JUAN, LocalDate.of(2024, 5, 1));
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        publicar();

        assertThat(avisos()).singleElement()
                .satisfies(a -> assertThat(a.getUsernameDestino()).isEqualTo("zz.juan.reciente"));
    }

    @Test
    void unaCuentaSinFechaDeCreacionPierdeFrenteAUnaConFecha() {
        personaConCorreo("zz.juan.sinfecha", CEDULA_JUAN, NOMBRE_JUAN, null);
        personaConCorreo("zz.juan.datada", CEDULA_JUAN, NOMBRE_JUAN, LocalDate.of(2021, 3, 3));
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        publicar();

        assertThat(avisos()).singleElement()
                .satisfies(a -> assertThat(a.getUsernameDestino()).isEqualTo("zz.juan.datada"));
    }

    @Test
    void unaCuentaInactivaNuncaEsElDestino() {
        personaConCorreo("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, LocalDate.of(2020, 1, 1));
        DirectorioAd masNueva = personaConCorreo("zz.juan.baja", CEDULA_JUAN, NOMBRE_JUAN, LocalDate.of(2024, 1, 1));
        masNueva.inactivar();
        directorioRepository.save(masNueva);
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        publicar();

        assertThat(avisos()).singleElement()
                .satisfies(a -> assertThat(a.getUsernameDestino()).isEqualTo("zz.juan"));
    }

    @Test
    void quienNoTieneCorreoQuedaOmitidoYNoCuentaComoEncolado() {
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);   // sin correo
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        PublicacionDto resultado = publicar();

        assertThat(resultado.avisosEncolados()).isZero();
        assertThat(avisos()).singleElement().satisfies(a -> {
            assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
            assertThat(a.getUltimoError()).isEqualTo("No hay una cuenta activa con correo en el AD");
            assertThat(a.getEmailDestino()).isNull();
        });
    }

    @Test
    void generarEsIdempotenteYQuedaRegistrado() {
        prepararTresPersonas();
        publicar();

        ResultadoEncoladoDto resultado = notificaciones.generar(PERIODO);

        assertThat(resultado.creadas()).isZero();
        assertThat(resultado.yaExistentes()).isEqualTo(3);
        assertThat(avisos()).hasSize(3);
        assertThat(acciones(TipoAccion.NOTIFICACIONES_GENERADAS)).isEqualTo(1);
    }

    @Test
    void generarDaOtraOportunidadAQuienAhoraTieneCorreo() {
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);   // sin correo
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
        publicar();
        assertThat(avisoDe(CEDULA_JUAN).getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        DirectorioAd juan = directorioRepository.findByUsernameAd("zz.juan").orElseThrow();
        juan.setEmail("zz.juan@coopsanjose.fin.ec");
        directorioRepository.save(juan);
        em.flush();

        ResultadoEncoladoDto resultado = notificaciones.generar(PERIODO);

        assertThat(resultado.creadas()).isEqualTo(1);
        assertThat(resultado.pendientes()).isEqualTo(1);
        assertThat(avisos()).singleElement().satisfies(a -> {   // el mismo aviso revive: no se duplica
            assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
            assertThat(a.getEmailDestino()).isEqualTo("zz.juan@coopsanjose.fin.ec");
        });
    }

    @Test
    void generarYReintentarExigenUnPeriodoPublicado() {
        prepararTresPersonas();   // cargado, sin publicar

        assertThatThrownBy(() -> notificaciones.generar(PERIODO))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("aún no está publicado");
        assertThatThrownBy(() -> notificaciones.reintentar(PERIODO))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("aún no está publicado");
        assertThat(notificaciones.informe(PERIODO).total()).isZero();
    }

    @Test
    void elInformeDiceQueLePasoACadaAvisoSinExponerDatosPersonales() {
        escenarioConUnFallidoYUnOmitido();

        InformeNotificacionesDto informe = notificaciones.informe(PERIODO);

        assertThat(informe.habilitadas()).isTrue();
        assertThat(informe.total()).isEqualTo(3);
        assertThat(informe.enviadas()).isEqualTo(1);
        assertThat(informe.fallidas()).isEqualTo(1);
        assertThat(informe.omitidas()).isEqualTo(1);
        assertThat(informe.pendientes()).isZero();
        assertThat(informe.problemas()).extracting(ProblemaNotificacionDto::nombre)
                .containsExactly(NOMBRE_MARIA, NOMBRE_LUIS);
        assertThat(informe.problemas()).extracting(ProblemaNotificacionDto::estado).containsExactly("FALLIDA", "OMITIDA");
        assertThat(informe.toString()).doesNotContain("@").doesNotContain(CEDULA_MARIA).doesNotContain(CEDULA_LUIS);
    }

    @Test
    void elInformeDiceSiElEnvioEstaListoParaQueNoSeConfundaUnAvisoQueEsperaConUnoQueFalla() {
        escenarioConUnFallidoYUnOmitido();

        when(enviador.estaConfigurado()).thenReturn(false);
        InformeNotificacionesDto sinConfigurar = notificaciones.informe(PERIODO);
        assertThat(sinConfigurar.envioConfigurado()).isFalse();
        assertThat(sinConfigurar.despachoAutomatico()).isFalse();   // la base de las pruebas lo apaga (SoporteRolesPago)
        assertThat(sinConfigurar.modoCorreo()).isIn("API", "SIMULADO");

        when(enviador.estaConfigurado()).thenReturn(true);
        assertThat(notificaciones.informe(PERIODO).envioConfigurado()).isTrue();
    }

    @Test
    void reintentarDevuelveLosFallidosALaColaYSoloEsos() {
        escenarioConUnFallidoYUnOmitido();

        long reintentadas = notificaciones.reintentar(PERIODO);

        assertThat(reintentadas).isEqualTo(1);
        assertThat(avisoDe(CEDULA_MARIA)).satisfies(a -> {
            assertThat(a.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
            assertThat(a.getIntentos()).isZero();
            assertThat(a.getUltimoError()).isNull();
        });
        assertThat(avisoDe(CEDULA_JUAN).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(avisoDe(CEDULA_LUIS).getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        assertThat(acciones(TipoAccion.NOTIFICACIONES_REINTENTADAS)).isEqualTo(1);
    }
}
