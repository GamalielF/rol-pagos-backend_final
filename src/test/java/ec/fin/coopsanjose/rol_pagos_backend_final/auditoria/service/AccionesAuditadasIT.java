package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.PdfDePrueba;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.GestionRolesPagoService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.PublicacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada acción de RRHH deja su evento. Solo se miran los del período de prueba (2099-03). */
class AccionesAuditadasIT extends SoporteRolesPago {

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private AccionRolPagoRepository accionRepository;
    @Autowired private GestionRolesPagoService gestion;
    @Autowired private PublicacionService publicacion;

    @BeforeEach
    void escenario() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private List<AccionRolPago> acciones() {
        em.flush();
        return accionRepository.findAll().stream()
                .filter(a -> "2099-03".equals(a.getPeriodo()))
                .sorted(Comparator.comparing(AccionRolPago::getId))
                .toList();
    }

    @Test
    void laCargaDejaUnEventoDeLoteConSusConteos() {
        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo("rol_malo.pdf", new byte[]{1})));

        assertThat(acciones()).singleElement().satisfies(a -> {
            assertThat(a.getTipoAccion()).isEqualTo(TipoAccion.CARGA_LOTE);
            assertThat(a.getDetalle()).isEqualTo("archivos=2, cargados=1, reemplazados=0, sinCambios=0, rechazados=1");
            assertThat(a.getRolPagoId()).isNull();
        });
    }

    @Test
    void unReemplazoDejaSuEventoConLasHuellas() {
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
        Long id = rolDe(CEDULA_JUAN).getId();
        String huellaAntes = rolDe(CEDULA_JUAN).getHashSha256();

        cargarUno(CEDULA_JUAN + ".pdf", PdfDePrueba.crear("LIQUIDACION DE SUELDO", "MES AÑO", MES_PERIODO, NOMBRE_JUAN, "BONO 10,00"));
        String huellaDespues = rolDe(CEDULA_JUAN).getHashSha256();

        List<AccionRolPago> eventos = acciones();
        assertThat(eventos).filteredOn(a -> a.getTipoAccion() == TipoAccion.CARGA_LOTE).hasSize(2);
        assertThat(eventos).filteredOn(a -> a.getTipoAccion() == TipoAccion.ROL_REEMPLAZADO).singleElement().satisfies(a -> {
            assertThat(a.getRolPagoId()).isEqualTo(id);
            assertThat(a.getDetalle()).isEqualTo("Contenido reemplazado (huella " + huellaAntes.substring(0, 8)
                    + " -> " + huellaDespues.substring(0, 8) + ")");
        });
    }

    @Test
    void reponerUnRolInactivoConElMismoArchivoSeRegistraComoActivacion() {
        byte[] pdf = rol(MES_PERIODO, NOMBRE_JUAN);
        cargarUno(CEDULA_JUAN + ".pdf", pdf);
        gestion.inactivar(PERIODO, rolDe(CEDULA_JUAN).getId());

        cargarUno(CEDULA_JUAN + ".pdf", pdf);   // el mismo archivo, pero el rol estaba retirado

        List<AccionRolPago> eventos = acciones();
        assertThat(eventos).extracting(AccionRolPago::getTipoAccion).containsExactly(
                TipoAccion.CARGA_LOTE, TipoAccion.ROL_INACTIVADO, TipoAccion.ROL_ACTIVADO, TipoAccion.CARGA_LOTE);
        assertThat(eventos.get(2).getDetalle()).isEqualTo("Reactivado al volver a cargar el mismo archivo");
    }

    @Test
    void inactivarYActivarManualesDejanSusEventos() {
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
        Long id = rolDe(CEDULA_JUAN).getId();

        gestion.inactivar(PERIODO, id);
        gestion.activar(PERIODO, id);

        List<AccionRolPago> eventos = acciones();
        assertThat(eventos).extracting(AccionRolPago::getTipoAccion)
                .containsExactly(TipoAccion.CARGA_LOTE, TipoAccion.ROL_INACTIVADO, TipoAccion.ROL_ACTIVADO);
        assertThat(eventos.get(1).getRolPagoId()).isEqualTo(id);
        assertThat(eventos.get(1).getDetalle()).isEqualTo("Retirado por RRHH antes de publicar");
        assertThat(eventos.get(2).getRolPagoId()).isEqualTo(id);
        assertThat(eventos.get(2).getDetalle()).isEqualTo("Repuesto por RRHH antes de publicar");
    }

    @Test
    void publicarDejaSuEvento() {
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        publicacion.publicar(PERIODO, false, "zz.rrhh");

        assertThat(acciones()).filteredOn(a -> a.getTipoAccion() == TipoAccion.PERIODO_PUBLICADO)
                .singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo("roles=1, sinRol=0"));
    }

    @Test
    void publicarConFaltantesConfirmadosLoDice() {
        persona("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, true);   // sin rol
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        publicacion.publicar(PERIODO, true, "zz.rrhh");

        assertThat(acciones()).filteredOn(a -> a.getTipoAccion() == TipoAccion.PERIODO_PUBLICADO)
                .singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo("roles=1, sinRol=1, faltantes confirmados"));
    }

    @Test
    void elActorYLaIpSalenDelContextoDeLaPeticion() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioAutenticado("zz.rrhh", null, null, Set.of(Rol.RRHH)), null, List.of()));
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr("10.1.2.3");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));

        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        assertThat(acciones()).singleElement().satisfies(a -> {
            assertThat(a.getUsernameAd()).isEqualTo("zz.rrhh");
            assertThat(a.getIpOrigen()).isEqualTo("10.1.2.3");
        });
    }

    @Test
    void sinSesionElActorEsSistemaYNoHayIp() {
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        assertThat(acciones()).singleElement().satisfies(a -> {
            assertThat(a.getUsernameAd()).isEqualTo("SISTEMA");
            assertThat(a.getIpOrigen()).isNull();
        });
    }

    @Test
    void noSeRegistraLoQueNoOcurrio() {
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
        Long id = rolDe(CEDULA_JUAN).getId();
        publicacion.publicar(PERIODO, false, "zz.rrhh");

        assertThatThrownBy(() -> gestion.inactivar(PERIODO, id)).isInstanceOf(ConflictoException.class);

        assertThat(acciones()).extracting(AccionRolPago::getTipoAccion).doesNotContain(TipoAccion.ROL_INACTIVADO);
    }
}
