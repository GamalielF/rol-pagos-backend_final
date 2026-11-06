package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.SoporteNotificaciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.EstadoNotificacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity.NotificacionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.EstadoCarga;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoArchivoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoCargaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.PdfDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Corregir un período YA publicado: reemplazar, agregar, retirar y reponer roles, avisar otra vez y dejar rastro. */
class CorreccionDePeriodoPublicadoIT extends SoporteNotificaciones {

    private static final String MOTIVO = "Se subió el rol equivocado";
    private static final String CEDULA_PEDRO = "1710001239";
    private static final String NOMBRE_PEDRO = "RAMOS VERA PEDRO LUIS";

    @Autowired private AccionRolPagoRepository accionRepository;
    @Autowired private GestionRolesPagoService gestion;

    /** Tres personas con rol, el período publicado y sus tres avisos ya ENVIADOS: la situación real de una corrección. */
    @BeforeEach
    void periodoPublicadoYAvisosEnviados() {
        prepararTresPersonas();
        publicar();
        marcarEnviado(CEDULA_JUAN);
        marcarEnviado(CEDULA_MARIA);
        marcarEnviado(CEDULA_LUIS);
    }

    private void marcarEnviado(String cedula) {
        NotificacionRolPago aviso = avisoDe(cedula);
        aviso.setEstado(EstadoNotificacion.ENVIADA);
        aviso.setIntentos(1);
        aviso.setEnviadaEn(LocalDateTime.now(clock).minusDays(1));
        avisoRepository.save(aviso);
        em.flush();
        em.clear();
    }

    /** El mismo rol, pero con otro contenido (otra huella). */
    private static byte[] corregido(String nombre) {
        return PdfDePrueba.crear("LIQUIDACION CORREGIDA", "MES AÑO", MES_PERIODO, nombre);
    }

    private List<AccionRolPago> acciones() {
        em.flush();
        return accionRepository.findAll().stream()
                .filter(a -> "2099-03".equals(a.getPeriodo()))
                .sorted(Comparator.comparing(AccionRolPago::getId))
                .toList();
    }

    // ------------------------------------------------------------ el motivo es obligatorio

    @Test
    void sinMotivoUnPeriodoPublicadoNoSeCorrige() {
        String huellaAntes = rolDe(CEDULA_JUAN).getHashSha256();

        assertThatThrownBy(() -> carga.cargar(PERIODO, List.of(archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN)))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("motivo");
        assertThatThrownBy(() -> carga.cargar(PERIODO, List.of(archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN))), false, "corto"))
                .isInstanceOf(SolicitudInvalidaException.class);

        assertThat(rolDe(CEDULA_JUAN).getHashSha256()).isEqualTo(huellaAntes);
        assertThat(avisoDe(CEDULA_JUAN).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    // ------------------------------------------------------------ reemplazar

    @Test
    void corregirReemplazaElRolYDevuelveSuAvisoALaCola() {
        String huellaAntes = rolDe(CEDULA_JUAN).getHashSha256();

        ResultadoCargaDto resultado = carga.cargar(PERIODO, List.of(archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN))), false, MOTIVO);

        assertThat(resultado.simulacion()).isFalse();
        ResultadoArchivoDto juan = resultado.resultados().get(0);
        assertThat(juan.estado()).isEqualTo(EstadoCarga.REEMPLAZADO);
        assertThat(juan.titular()).isEqualTo(NOMBRE_JUAN);
        assertThat(rolDe(CEDULA_JUAN).getHashSha256()).isNotEqualTo(huellaAntes);

        NotificacionRolPago aviso = avisoDe(CEDULA_JUAN);
        assertThat(aviso.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(aviso.getIntentos()).isZero();
        assertThat(aviso.getReenvios()).isEqualTo(1);
        assertThat(aviso.getEmailDestino()).isEqualTo("zz.juan@coopsanjose.fin.ec");
        assertThat(avisoDe(CEDULA_MARIA).getEstado()).as("María no se tocó").isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(avisoDe(CEDULA_LUIS).getEstado()).as("Luis no se tocó").isEqualTo(EstadoNotificacion.ENVIADA);
    }

    @Test
    void elSiguienteDespachoEnviaOtraVezSoloElAvisoCorregido() {
        carga.cargar(PERIODO, List.of(archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN))), false, MOTIVO);

        assertThat(despacho.despacharLote().enviadas()).isEqualTo(1);

        verify(enviador, times(1)).enviar(eq("zz.juan@coopsanjose.fin.ec"), any(), any(), any());
        assertThat(avisoDe(CEDULA_JUAN).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    @Test
    void unRolIdenticoNoVuelveAEnviarse() {
        ResultadoCargaDto resultado = carga.cargar(PERIODO,
                List.of(archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA))), false, MOTIVO);

        assertThat(resultado.resultados().get(0).estado()).isEqualTo(EstadoCarga.SIN_CAMBIOS);
        NotificacionRolPago aviso = avisoDe(CEDULA_MARIA);
        assertThat(aviso.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(aviso.getReenvios()).isZero();
    }

    @Test
    void volverASubirTodoElPeriodoSoloReemplazaLoQueCambio() {
        ResultadoCargaDto resultado = carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA)),
                archivo(CEDULA_LUIS + ".pdf", rol(MES_PERIODO, NOMBRE_LUIS))), false, MOTIVO);

        assertThat(resultado.reemplazados()).isEqualTo(1);
        assertThat(resultado.sinCambios()).isEqualTo(2);
        assertThat(avisos()).extracting(NotificacionRolPago::getEstado)
                .containsExactlyInAnyOrder(EstadoNotificacion.PENDIENTE, EstadoNotificacion.ENVIADA, EstadoNotificacion.ENVIADA);
    }

    // ------------------------------------------------------------ agregar

    @Test
    void unRolNuevoDespuesDePublicarSeAgregaYSeLeAvisa() {
        personaConCorreo("zz.pedro", CEDULA_PEDRO, NOMBRE_PEDRO, LocalDate.of(2023, 1, 1));

        ResultadoCargaDto resultado = carga.cargar(PERIODO,
                List.of(archivo(CEDULA_PEDRO + ".pdf", rol(MES_PERIODO, NOMBRE_PEDRO))), false, MOTIVO);

        assertThat(resultado.resultados().get(0).estado()).isEqualTo(EstadoCarga.CARGADO);
        NotificacionRolPago aviso = avisoDe(CEDULA_PEDRO);
        assertThat(aviso.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(aviso.getEmailDestino()).isEqualTo("zz.pedro@coopsanjose.fin.ec");
        assertThat(acciones()).extracting(AccionRolPago::getTipoAccion).contains(TipoAccion.ROL_AGREGADO);
    }

    // ------------------------------------------------------------ la vista previa

    @Test
    void laRevisionNoCambiaNadaYNoPideMotivo() {
        personaConCorreo("zz.pedro", CEDULA_PEDRO, NOMBRE_PEDRO, LocalDate.of(2023, 1, 1));
        String huellaAntes = rolDe(CEDULA_JUAN).getHashSha256();
        long accionesAntes = acciones().size();

        ResultadoCargaDto resultado = carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA)),
                archivo(CEDULA_PEDRO + ".pdf", rol(MES_PERIODO, NOMBRE_PEDRO)),
                archivo("rol_malo.pdf", new byte[]{1})), true, null);

        assertThat(resultado.simulacion()).isTrue();
        assertThat(resultado.resultados()).extracting(ResultadoArchivoDto::estado).containsExactly(
                EstadoCarga.REEMPLAZADO, EstadoCarga.SIN_CAMBIOS, EstadoCarga.CARGADO, EstadoCarga.RECHAZADO);
        assertThat(resultado.resultados().get(0).titular()).isEqualTo(NOMBRE_JUAN);
        assertThat(resultado.resultados().get(0).mensaje()).contains("se va a reemplazar");

        assertThat(rolDe(CEDULA_JUAN).getHashSha256()).as("el rol no cambió").isEqualTo(huellaAntes);
        assertThat(avisos()).as("no se creó ningún aviso nuevo").hasSize(3);
        assertThat(avisoDe(CEDULA_JUAN).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(acciones()).as("no se registró nada").hasSize((int) accionesAntes);
    }

    @Test
    void laRevisionAnunciaExactamenteLoQueDespuesPasaDeVerdad() {
        personaConCorreo("zz.pedro", CEDULA_PEDRO, NOMBRE_PEDRO, LocalDate.of(2023, 1, 1));
        List<org.springframework.web.multipart.MultipartFile> archivos = List.of(
                archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA)),
                archivo(CEDULA_PEDRO + ".pdf", rol(MES_PERIODO, NOMBRE_PEDRO)));

        List<EstadoCarga> anunciado = carga.cargar(PERIODO, archivos, true, null).resultados().stream()
                .map(ResultadoArchivoDto::estado).toList();
        List<EstadoCarga> real = carga.cargar(PERIODO, archivos, false, MOTIVO).resultados().stream()
                .map(ResultadoArchivoDto::estado).toList();

        assertThat(real).isEqualTo(anunciado);
    }

    // ------------------------------------------------------------ la auditoría

    @Test
    void laCorreccionDejaSuRastroEnLaAuditoria() {
        personaConCorreo("zz.pedro", CEDULA_PEDRO, NOMBRE_PEDRO, LocalDate.of(2023, 1, 1));

        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN)),
                archivo(CEDULA_PEDRO + ".pdf", rol(MES_PERIODO, NOMBRE_PEDRO))), false, MOTIVO);

        List<AccionRolPago> eventos = acciones();
        assertThat(eventos).extracting(AccionRolPago::getTipoAccion)
                .contains(TipoAccion.CARGA_CORRECCION, TipoAccion.ROL_REEMPLAZADO, TipoAccion.ROL_AGREGADO);
        AccionRolPago lote = eventos.stream().filter(a -> a.getTipoAccion() == TipoAccion.CARGA_CORRECCION).findFirst().orElseThrow();
        assertThat(lote.getDetalle()).contains("motivo=" + MOTIVO).contains("reemplazados=1").contains("cargados=1");
        AccionRolPago reemplazo = eventos.stream().filter(a -> a.getTipoAccion() == TipoAccion.ROL_REEMPLAZADO).findFirst().orElseThrow();
        assertThat(reemplazo.getDetalle()).contains("corrección de un período publicado");
        assertThat(reemplazo.getRolPagoId()).isEqualTo(rolDe(CEDULA_JUAN).getId());
    }

    // ------------------------------------------------------------ quitar y reponer

    @Test
    void retirarYReponerUnRolPublicadoExigeMotivoYDejaRastro() {
        Long id = rolDe(CEDULA_JUAN).getId();

        assertThatThrownBy(() -> gestion.inactivar(PERIODO, id))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("motivo");

        gestion.inactivar(PERIODO, id, "Se subió a la persona equivocada");
        assertThat(rolDe(CEDULA_JUAN).getEstadoRegistro()).isEqualTo(EstadoRegistro.INACTIVO);

        gestion.activar(PERIODO, id, "Era correcto, se repone");
        assertThat(rolDe(CEDULA_JUAN).getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);

        assertThat(acciones()).filteredOn(a -> a.getTipoAccion() == TipoAccion.ROL_INACTIVADO)
                .extracting(AccionRolPago::getDetalle)
                .containsExactly("Retirado por RRHH después de publicar; motivo=Se subió a la persona equivocada");
        assertThat(acciones()).filteredOn(a -> a.getTipoAccion() == TipoAccion.ROL_ACTIVADO)
                .extracting(AccionRolPago::getDetalle)
                .containsExactly("Repuesto por RRHH después de publicar; motivo=Era correcto, se repone");
    }

    @Test
    void unRolRetiradoSeReactivaAlCorregirloYSuAvisoVuelveALaCola() {
        Long id = rolDe(CEDULA_JUAN).getId();
        gestion.inactivar(PERIODO, id, "Se subió a la persona equivocada");

        ResultadoCargaDto resultado = carga.cargar(PERIODO, List.of(archivo(CEDULA_JUAN + ".pdf", corregido(NOMBRE_JUAN))), false, MOTIVO);

        assertThat(resultado.resultados().get(0).estado()).isEqualTo(EstadoCarga.REEMPLAZADO);
        assertThat(rolDe(CEDULA_JUAN).getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(avisoDe(CEDULA_JUAN).getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
    }
}
