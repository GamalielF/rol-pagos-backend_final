package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response.UsuarioAdminDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.entity.AsignacionRol;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.repository.AsignacionRolRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.PaginaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Permiso;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdministracionUsuariosServiceIT extends SoporteRolesPago {

    @Autowired private AdministracionUsuariosService servicio;
    @Autowired private RolesUsuarioService roles;
    @Autowired private AsignacionRolRepository asignaciones;
    @Autowired private AccionRolPagoRepository acciones;

    @BeforeEach
    void escenario() {
        SecurityContextHolder.clearContext();
        persona("zz.pedro", CEDULA_JUAN, NOMBRE_JUAN, true);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    /** Solo los eventos de los usuarios de prueba (zz.*): la bitácora real de desarrollo no interviene. */
    private List<AccionRolPago> eventos(TipoAccion tipo) {
        em.flush();
        return acciones.findAll().stream()
                .filter(a -> a.getTipoAccion() == tipo && a.getDetalle() != null && a.getDetalle().startsWith("usuario=zz."))
                .sorted(Comparator.comparing(AccionRolPago::getId))
                .toList();
    }

    private List<AsignacionRol> filasDe(String usuario) {
        em.flush();
        return asignaciones.findAll().stream().filter(a -> usuario.equals(a.getUsernameAd())).toList();
    }

    private void actuarComo(String usuario) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UsuarioAutenticado(usuario, null, null, Set.of(Rol.ADMIN)), null, List.of()));
    }

    // ------------------------------------------------------------ otorgar

    @Test
    void otorgarRrhhPorDefectoConcedeElPermisoYQuedaRegistrado() {
        UsuarioAdminDto dto = servicio.otorgar("zz.pedro", Rol.RRHH, null);

        assertThat(dto.estadoDirectorio()).isEqualTo("ACTIVO");
        assertThat(dto.roles()).singleElement().satisfies(r -> {
            assertThat(r.rol()).isEqualTo("RRHH");
            assertThat(r.origen()).isEqualTo("ASIGNADO");
            assertThat(r.verRolesAjenos()).isTrue();
        });
        assertThat(roles.resolver("zz.pedro").permisos()).containsExactly(Permiso.VER_ROLES_AJENOS);
        assertThat(eventos(TipoAccion.USUARIO_ROL_OTORGADO)).singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo("usuario=zz.pedro, rol=RRHH, verRolesAjenos=true"));
    }

    @Test
    void otorgarRrhhSinElPermisoDeVerRolesAjenos() {
        UsuarioAdminDto dto = servicio.otorgar("zz.pedro", Rol.RRHH, false);

        assertThat(dto.roles()).singleElement().satisfies(r -> assertThat(r.verRolesAjenos()).isFalse());
        assertThat(roles.resolver("zz.pedro").roles()).contains(Rol.RRHH);
        assertThat(roles.resolver("zz.pedro").permisos()).isEmpty();
        assertThat(eventos(TipoAccion.USUARIO_ROL_OTORGADO)).singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo("usuario=zz.pedro, rol=RRHH, verRolesAjenos=false"));
    }

    @Test
    void otorgarEsIdempotenteYNoDuplicaFilasNiEventos() {
        servicio.otorgar("zz.pedro", Rol.RRHH, null);
        servicio.otorgar("zz.pedro", Rol.RRHH, null);

        assertThat(filasDe("zz.pedro")).hasSize(1);
        assertThat(eventos(TipoAccion.USUARIO_ROL_OTORGADO)).hasSize(1);
    }

    @Test
    void cambiarElPermisoDeUnRrhhExistenteRegistraOtroEvento() {
        servicio.otorgar("zz.pedro", Rol.RRHH, true);
        servicio.otorgar("zz.pedro", Rol.RRHH, false);

        assertThat(filasDe("zz.pedro")).singleElement().satisfies(a -> assertThat(a.puedeVerRolesAjenos()).isFalse());
        assertThat(eventos(TipoAccion.USUARIO_ROL_OTORGADO)).hasSize(2);
    }

    @Test
    void elAdminSiempreTieneElPermisoSinImportarLoPedido() {
        UsuarioAdminDto dto = servicio.otorgar("zz.pedro", Rol.ADMIN, false);

        assertThat(dto.roles()).singleElement().satisfies(r -> {
            assertThat(r.rol()).isEqualTo("ADMIN");
            assertThat(r.verRolesAjenos()).isTrue();
        });
        assertThat(roles.resolver("zz.pedro").roles()).contains(Rol.ADMIN);
        assertThat(eventos(TipoAccion.USUARIO_ROL_OTORGADO)).singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo("usuario=zz.pedro, rol=ADMIN"));
    }

    @Test
    void noSeOtorgaAQuienNoExisteNiAQuienEstaInactivoEnElAd() {
        persona("zz.baja", CEDULA_MARIA, NOMBRE_MARIA, false);

        assertThatThrownBy(() -> servicio.otorgar("zz.no.existe", Rol.RRHH, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.otorgar("zz.baja", Rol.RRHH, null))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("inactivo");
        assertThat(filasDe("zz.baja")).isEmpty();
    }

    @Test
    void soloSeAsignanAdminYRrhhYElCodigoDebeSerValido() {
        assertThatThrownBy(() -> servicio.otorgar("zz.pedro", Rol.EMPLEADO, null))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("Solo se asignan");
        assertThatThrownBy(() -> servicio.otorgar("a b", Rol.RRHH, null))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("inválido");
        assertThatThrownBy(() -> servicio.otorgar("../etc", Rol.RRHH, null))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.revocar("zz.pedro", Rol.EMPLEADO))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void unAdministradorDeArranqueNoSeReasignaNiSeRevoca() {
        persona("fsepa", CEDULA_MARIA, NOMBRE_MARIA, true);

        assertThatThrownBy(() -> servicio.otorgar("fsepa", Rol.ADMIN, null))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("arranque");
        assertThatThrownBy(() -> servicio.revocar("fsepa", Rol.ADMIN))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("arranque");
        assertThat(filasDe("fsepa")).isEmpty();
    }

    // ------------------------------------------------------------ revocar

    @Test
    void revocarInactivaLaAsignacionSinBorrarlaYSurteEfecto() {
        servicio.otorgar("zz.pedro", Rol.RRHH, null);

        UsuarioAdminDto dto = servicio.revocar("zz.pedro", Rol.RRHH);

        assertThat(dto.roles()).isEmpty();
        assertThat(roles.resolver("zz.pedro").roles()).containsExactly(Rol.EMPLEADO);
        assertThat(filasDe("zz.pedro")).singleElement()
                .satisfies(a -> assertThat(a.getEstadoRegistro()).isEqualTo(EstadoRegistro.INACTIVO));
        assertThat(eventos(TipoAccion.USUARIO_ROL_REVOCADO)).singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo("usuario=zz.pedro, rol=RRHH"));
    }

    @Test
    void revocarUnRolQueNoSeTieneDa404() {
        assertThatThrownBy(() -> servicio.revocar("zz.pedro", Rol.RRHH)).isInstanceOf(RecursoNoEncontradoException.class);

        servicio.otorgar("zz.pedro", Rol.RRHH, null);
        servicio.revocar("zz.pedro", Rol.RRHH);

        assertThatThrownBy(() -> servicio.revocar("zz.pedro", Rol.RRHH)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void volverAOtorgarDespuesDeRevocarReactivaLaMismaFila() {
        servicio.otorgar("zz.pedro", Rol.RRHH, null);
        servicio.revocar("zz.pedro", Rol.RRHH);

        servicio.otorgar("zz.pedro", Rol.RRHH, null);

        assertThat(filasDe("zz.pedro")).singleElement()
                .satisfies(a -> assertThat(a.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO));
        assertThat(roles.resolver("zz.pedro").roles()).contains(Rol.RRHH);
    }

    @Test
    void nadieSeQuitaElAdminASiMismoPeroUnAdminSiPuedeQuitarloAOtro() {
        persona("zz.admin1", CEDULA_MARIA, NOMBRE_MARIA, true);
        persona("zz.admin2", "1723456784", "SILVA TORRES LUIS ALBERTO", true);
        servicio.otorgar("zz.admin1", Rol.ADMIN, null);
        servicio.otorgar("zz.admin2", Rol.ADMIN, null);
        actuarComo("zz.admin1");

        assertThatThrownBy(() -> servicio.revocar("zz.admin1", Rol.ADMIN))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("a sí mismo");
        assertThat(servicio.revocar("zz.admin2", Rol.ADMIN).roles()).isEmpty();
        assertThat(roles.resolver("zz.admin1").roles()).contains(Rol.ADMIN);
    }

    // ------------------------------------------------------------ consultas

    @Test
    void buscarFiltraPorNombreOCodigoSoloActivosYNeutralizaLosComodines() {
        persona("zz.ana", CEDULA_MARIA, "PEREZ ANA LUCIA", true);
        persona("zz.beto", "1723456784", "RUIZ BETO", true);
        persona("zz.baja", "1711111110", "PEREZ ANA BAJA", false);
        servicio.otorgar("zz.ana", Rol.RRHH, null);

        assertThat(servicio.buscar("ana", 0, 50).elementos()).extracting(UsuarioAdminDto::username).containsExactly("zz.ana");
        assertThat(servicio.buscar("ZZ.BE", 0, 50).elementos()).extracting(UsuarioAdminDto::username).containsExactly("zz.beto");
        assertThat(servicio.buscar("nadie", 0, 50).totalElementos()).isZero();
        // los comodines de LIKE tecleados no se comportan como comodines: equivalen a "sin filtro" (3 activos)
        assertThat(servicio.buscar("%", 0, 50).totalElementos()).isEqualTo(3);
        assertThat(servicio.buscar("_", 0, 50).totalElementos()).isEqualTo(3);
        assertThat(servicio.buscar(null, 0, 50).totalElementos()).isEqualTo(3);
        // el rol aparece junto al usuario, y nunca se muestra a los inactivos
        assertThat(servicio.buscar("zz.ana", 0, 50).elementos()).singleElement()
                .satisfies(u -> assertThat(u.roles()).extracting(r -> r.rol()).containsExactly("RRHH"));
        assertThat(servicio.buscar("zz.baja", 0, 50).totalElementos()).isZero();
    }

    @Test
    void laPaginacionSeAcota() {
        PaginaDto<UsuarioAdminDto> enorme = servicio.buscar(null, 0, 10_000);
        PaginaDto<UsuarioAdminDto> raro = servicio.buscar(null, -3, 0);

        assertThat(enorme.tamano()).isEqualTo(200);
        assertThat(raro.pagina()).isZero();
        assertThat(raro.tamano()).isEqualTo(50);
    }

    @Test
    void privilegiadosIncluyeArranqueAsignadosYDadosDeBaja() {
        servicio.otorgar("zz.pedro", Rol.RRHH, null);
        persona("zz.otro", CEDULA_MARIA, NOMBRE_MARIA, true);
        servicio.otorgar("zz.otro", Rol.RRHH, null);
        DirectorioAd otro = directorioRepository.findByUsernameAd("zz.otro").orElseThrow();
        otro.inactivar();   // lo dieron de baja en el AD: sigue apareciendo para poder quitarle el rol
        directorioRepository.save(otro);
        em.flush();

        List<UsuarioAdminDto> lista = servicio.privilegiados();

        assertThat(lista).filteredOn(u -> u.username().equals("fsepa")).singleElement().satisfies(u -> {
            assertThat(u.roles()).singleElement().satisfies(r -> {
                assertThat(r.rol()).isEqualTo("ADMIN");
                assertThat(r.origen()).isEqualTo("CONFIGURACION");
            });
            assertThat(u.estadoDirectorio()).isEqualTo("NO_EXISTE");   // en la prueba el directorio está vacío
        });
        assertThat(lista).extracting(UsuarioAdminDto::username).contains("msavala", "zz.pedro", "zz.otro");
        assertThat(lista).filteredOn(u -> u.username().equals("zz.otro")).singleElement()
                .satisfies(u -> assertThat(u.estadoDirectorio()).isEqualTo("INACTIVO"));
    }
}