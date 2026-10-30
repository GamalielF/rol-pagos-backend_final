package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccesoRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.DetalleAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccesoRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.AccesoDenegadoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoPdfDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.YearMonth;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada acceso, permitido o denegado, deja su registro. Los nombres "zz.*" evitan chocar con datos reales. */
class ConsultaRolPagoServiceIT extends SoporteRolesPago {

    private static final YearMonth ABRIL = YearMonth.of(2099, 4);
    private static final String CEDULA_PERSONAL = "1723456784";
    private static final String IP = "10.0.0.5";

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private PublicacionService publicacion;
    @Autowired private ConsultaRolPagoService consulta;
    @Autowired private AccesoRolPagoRepository accesoRepository;

    @BeforeEach
    void escenario() {
        persona("zz.juan", CEDULA_JUAN, NOMBRE_JUAN, true);
        persona("zz.maria", CEDULA_MARIA, NOMBRE_MARIA, true);
        carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA))));
        publicacion.publicar(PERIODO, false, "zz.rrhh");
    }

    private static UsuarioAutenticado empleado(String username, String cedula) {
        return new UsuarioAutenticado(username, cedula, null, Set.of(Rol.EMPLEADO));
    }

    private static UsuarioAutenticado personal(String username, String cedula) {
        return new UsuarioAutenticado(username, cedula, null, Set.of(Rol.RRHH));
    }

    private List<AccesoRolPago> accesosDe(String username) {
        em.flush();
        return accesoRepository.findAll().stream().filter(a -> username.equals(a.getUsernameAd())).toList();
    }

    private RolPago rolDeEn(YearMonth periodo, String cedula) {
        em.flush();
        em.clear();
        Long idPeriodo = periodoRepository.findByFechaPeriodo(periodo.atDay(1)).orElseThrow().getId();
        return rolPagoRepository.findByPeriodo_IdAndCedula(idPeriodo, cedula).orElseThrow();
    }

    private void cargarDeAbril(String cedula, String nombre) {
        carga.cargar(ABRIL, List.of(archivo(cedula + ".pdf", rol("ABRIL 2099", nombre))));
    }

    // ------------------------------------------------------------ consulta propia

    @Test
    void elTitularRecibeSuRolYQuedaRegistrado() {
        RolPagoPdfDto dto = consulta.consultarPropio(empleado("zz.juan", CEDULA_JUAN), CEDULA_JUAN, null, IP);

        assertThat(dto.periodo()).isEqualTo("2099-03");
        assertThat(dto.nombreArchivo()).isEqualTo("rol-pago-2099-03.pdf").doesNotContain(CEDULA_JUAN);
        byte[] entregado = Base64.getDecoder().decode(dto.contenidoBase64());
        assertThat(entregado).isEqualTo(almacen.leer(rolDe(CEDULA_JUAN).getRutaArchivo()));
        assertThat(new String(entregado, 0, 5)).isEqualTo("%PDF-");
        assertThat(accesosDe("zz.juan")).singleElement().satisfies(a -> {
            assertThat(a.getTipoAcceso()).isEqualTo(TipoAcceso.CONSULTA_PROPIA);
            assertThat(a.getResultado()).isEqualTo(ResultadoAcceso.PERMITIDO);
            assertThat(a.getDetalle()).isEqualTo(DetalleAcceso.OK);
            assertThat(a.getIpOrigen()).isEqualTo(IP);
        });
    }

    @Test
    void conLaCedulaDeUnCompaneroSeDeniegaAunqueTengaRolPublicado() {
        assertThatThrownBy(() -> consulta.consultarPropio(empleado("zz.juan", CEDULA_JUAN), CEDULA_MARIA, null, IP))
                .isInstanceOf(AccesoDenegadoException.class);

        assertThat(accesosDe("zz.juan")).singleElement().satisfies(a -> {
            assertThat(a.getResultado()).isEqualTo(ResultadoAcceso.DENEGADO);
            assertThat(a.getDetalle()).isEqualTo(DetalleAcceso.CEDULA_NO_COINCIDE);
            assertThat(a.getCedulaConsultada()).isEqualTo(CEDULA_MARIA);
        });
    }

    @Test
    void unaCuentaSinCedulaEnElDirectorioNoPuedeConsultar() {
        assertThatThrownBy(() -> consulta.consultarPropio(empleado("zz.generica", null), CEDULA_JUAN, null, IP))
                .isInstanceOf(AccesoDenegadoException.class);

        assertThat(accesosDe("zz.generica")).singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo(DetalleAcceso.SIN_CEDULA_EN_DIRECTORIO));
    }

    @Test
    void soloSeEntregaLoPublicado() {
        cargarDeAbril(CEDULA_JUAN, NOMBRE_JUAN);   // cargado, pero NO publicado
        UsuarioAutenticado juan = empleado("zz.juan", CEDULA_JUAN);

        assertThat(consulta.consultarPropio(juan, CEDULA_JUAN, null, IP).periodo()).isEqualTo("2099-03");
        assertThatThrownBy(() -> consulta.consultarPropio(juan, CEDULA_JUAN, ABRIL, IP))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(consulta.listarPeriodosPropios(juan)).containsExactly("2099-03");
    }

    @Test
    void sinPeriodoSeEntregaElMasRecienteYConPeriodoElPedido() {
        cargarDeAbril(CEDULA_JUAN, NOMBRE_JUAN);
        publicacion.publicar(ABRIL, true, "zz.rrhh");
        UsuarioAutenticado juan = empleado("zz.juan", CEDULA_JUAN);

        assertThat(consulta.consultarPropio(juan, CEDULA_JUAN, null, IP).periodo()).isEqualTo("2099-04");
        assertThat(consulta.consultarPropio(juan, CEDULA_JUAN, PERIODO, IP).periodo()).isEqualTo("2099-03");
        assertThat(consulta.listarPeriodosPropios(juan)).containsExactly("2099-04", "2099-03");
    }

    @Test
    void unRolInactivoNoSeEntrega() {
        RolPago rol = rolDe(CEDULA_JUAN);
        rol.inactivar();
        rolPagoRepository.save(rol);
        em.flush();

        assertThatThrownBy(() -> consulta.consultarPropio(empleado("zz.juan", CEDULA_JUAN), CEDULA_JUAN, null, IP))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void siFaltaElArchivoSeRegistraYNoSeEntregaNada() {
        RolPago rol = rolDe(CEDULA_JUAN);
        rol.setRutaArchivo("2099-03/no-existe.pdf");
        rolPagoRepository.save(rol);
        em.flush();

        assertThatThrownBy(() -> consulta.consultarPropio(empleado("zz.juan", CEDULA_JUAN), CEDULA_JUAN, null, IP))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no está disponible");
        assertThat(accesosDe("zz.juan")).singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo(DetalleAcceso.ARCHIVO_NO_DISPONIBLE));
    }

    // ------------------------------------------------------------ RRHH / Admin

    @Test
    void personalVeElRolDeOtroConMotivoYQuedaRegistrado() {
        Long idJuan = rolDe(CEDULA_JUAN).getId();

        RolPagoPdfDto dto = consulta.verComoPersonal(personal("zz.rrhh", CEDULA_PERSONAL), PERIODO, idJuan,
                "Reclamo del empleado", IP);

        assertThat(dto.contenidoBase64()).isNotBlank();
        assertThat(accesosDe("zz.rrhh")).singleElement().satisfies(a -> {
            assertThat(a.getTipoAcceso()).isEqualTo(TipoAcceso.CONSULTA_AJENA);
            assertThat(a.getResultado()).isEqualTo(ResultadoAcceso.PERMITIDO);
            assertThat(a.getMotivo()).isEqualTo("Reclamo del empleado");
            assertThat(a.getCedulaConsultada()).isEqualTo(CEDULA_JUAN);
            assertThat(a.getRolPagoId()).isEqualTo(idJuan);
        });
    }

    @Test
    void siElPersonalVeSuPropioRolSeRegistraComoConsultaPropia() {
        Long idJuan = rolDe(CEDULA_JUAN).getId();

        consulta.verComoPersonal(personal("zz.rrhh", CEDULA_JUAN), PERIODO, idJuan, "Revisión de mi propio rol", IP);

        assertThat(accesosDe("zz.rrhh")).singleElement()
                .satisfies(a -> assertThat(a.getTipoAcceso()).isEqualTo(TipoAcceso.CONSULTA_PROPIA));
    }

    @Test
    void unEmpleadoNoPuedeVerRolesAjenos() {
        Long idMaria = rolDe(CEDULA_MARIA).getId();

        assertThatThrownBy(() -> consulta.verComoPersonal(empleado("zz.juan", CEDULA_JUAN), PERIODO, idMaria,
                "Curiosidad sobre el sueldo", IP)).isInstanceOf(AccesoDenegadoException.class);

        assertThat(accesosDe("zz.juan")).singleElement().satisfies(a -> {
            assertThat(a.getTipoAcceso()).isEqualTo(TipoAcceso.CONSULTA_AJENA);
            assertThat(a.getResultado()).isEqualTo(ResultadoAcceso.DENEGADO);
            assertThat(a.getDetalle()).isEqualTo(DetalleAcceso.SIN_PERMISO);
        });
    }

    @Test
    void elIdDeOtroPeriodoNoSirve() {
        Long idJuan = rolDe(CEDULA_JUAN).getId();

        assertThatThrownBy(() -> consulta.verComoPersonal(personal("zz.rrhh", CEDULA_PERSONAL), ABRIL, idJuan,
                "Motivo de la consulta", IP)).isInstanceOf(RecursoNoEncontradoException.class);

        assertThat(accesosDe("zz.rrhh")).singleElement()
                .satisfies(a -> assertThat(a.getDetalle()).isEqualTo(DetalleAcceso.ROL_NO_ENCONTRADO));
    }

    @Test
    void elPersonalPuedePrevisualizarUnPeriodoSinPublicar() {
        cargarDeAbril(CEDULA_JUAN, NOMBRE_JUAN);   // sin publicar
        Long idAbril = rolDeEn(ABRIL, CEDULA_JUAN).getId();

        RolPagoPdfDto dto = consulta.verComoPersonal(personal("zz.rrhh", CEDULA_PERSONAL), ABRIL, idAbril,
                "Verificación antes de publicar", IP);

        assertThat(dto.periodo()).isEqualTo("2099-04");
    }
}
