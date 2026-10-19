package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.request.FiltroAcciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.request.FiltroAccesos;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.AccesoAuditoriaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.ArchivoDescargaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.CoberturaPeriodoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.DenegadosPorUsuarioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.DetalleAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.AccesoDenegadoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.PaginaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.ConsultaRolPagoService;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service.PublicacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Los reportes agregan TODA la bitácora (incluida la real de desarrollo): por eso se filtra por usuarios "zz.*". */
class ReportesAuditoriaServiceIT extends SoporteRolesPago {

    private static final YearMonth ABRIL = YearMonth.of(2099, 4);
    private static final String CEDULA_PERSONAL = "1723456784";
    private static final String IP = "10.0.0.5";

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private ReportesAuditoriaService reportes;
    @Autowired private ConsultaRolPagoService consulta;
    @Autowired private PublicacionService publicacion;
    @Autowired private AccionRolPagoRepository accionRepository;

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

    private static FiltroAccesos sinFiltros() {
        return new FiltroAccesos(null, null, null, null, null, null);
    }

    private static FiltroAccesos deUsuario(String usuario) {
        return new FiltroAccesos(null, null, usuario, null, null, null);
    }

    private void intentoFallido(UsuarioAutenticado usuario, String cedula) {
        assertThatThrownBy(() -> consulta.consultarPropio(usuario, cedula, null, IP))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    // ------------------------------------------------------------ accesos

    @Test
    void filtraPorUsuarioYResultado() {
        UsuarioAutenticado juan = empleado("zz.juan", CEDULA_JUAN);
        consulta.consultarPropio(juan, CEDULA_JUAN, null, IP);
        intentoFallido(juan, CEDULA_MARIA);
        intentoFallido(juan, CEDULA_PERSONAL);

        PaginaDto<AccesoAuditoriaDto> denegados = reportes.accesos(
                new FiltroAccesos(null, null, "zz.juan", ResultadoAcceso.DENEGADO, null, null), 0, 50);

        assertThat(denegados.totalElementos()).isEqualTo(2);
        assertThat(denegados.elementos()).allSatisfy(a -> {
            assertThat(a.usuario()).isEqualTo("zz.juan");
            assertThat(a.resultado()).isEqualTo("DENEGADO");
            assertThat(a.detalle()).isEqualTo(DetalleAcceso.CEDULA_NO_COINCIDE.name());
        });
        assertThat(reportes.accesos(deUsuario("zz.juan"), 0, 50).totalElementos()).isEqualTo(3);
    }

    @Test
    void lasCedulasSalenEnmascaradasYElMotivoEsVisible() {
        Long idJuan = rolDe(CEDULA_JUAN).getId();
        consulta.verComoPersonal(personal("zz.rrhh", CEDULA_PERSONAL), PERIODO, idJuan, "Reclamo del empleado", IP);

        PaginaDto<AccesoAuditoriaDto> pagina = reportes.accesos(
                new FiltroAccesos(null, null, "zz.rrhh", null, TipoAcceso.CONSULTA_AJENA, null), 0, 50);

        assertThat(pagina.elementos()).singleElement().satisfies(a -> {
            assertThat(a.cedulaEnmascarada()).isEqualTo("17******75");
            assertThat(a.motivo()).isEqualTo("Reclamo del empleado");
            assertThat(a.ip()).isEqualTo(IP);
        });
        assertThat(pagina.toString()).doesNotContain(CEDULA_JUAN);
    }

    @Test
    void losDenegadosSeAgrupanPorUsuarioYCuentanLasCedulasDistintas() {
        UsuarioAutenticado sondeador = empleado("zz.sondeador", CEDULA_JUAN);
        intentoFallido(sondeador, CEDULA_MARIA);
        intentoFallido(sondeador, CEDULA_MARIA);        // repetida: no suma a las distintas
        intentoFallido(sondeador, CEDULA_PERSONAL);
        intentoFallido(sondeador, "1711111110");

        List<DenegadosPorUsuarioDto> desdeTres = reportes.denegados(null, null, 3);

        assertThat(desdeTres).anySatisfy(d -> {
            assertThat(d.usuario()).isEqualTo("zz.sondeador");
            assertThat(d.intentos()).isEqualTo(4);
            assertThat(d.cedulasDistintas()).isEqualTo(3);
            assertThat(d.ultimoIntento()).isNotNull();
        });
        assertThat(reportes.denegados(null, null, 5)).noneMatch(d -> "zz.sondeador".equals(d.usuario()));
    }

    @Test
    void elRangoYElPeriodoSeValidan() {
        assertThatThrownBy(() -> reportes.accesos(
                new FiltroAccesos(LocalDate.of(2099, 1, 10), LocalDate.of(2099, 1, 1), null, null, null, null), 0, 50))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("posterior");
        assertThatThrownBy(() -> reportes.accesos(
                new FiltroAccesos(LocalDate.of(2090, 1, 1), LocalDate.of(2099, 1, 1), null, null, null, null), 0, 50))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("366");
        assertThatThrownBy(() -> reportes.accesos(
                new FiltroAccesos(null, null, null, null, null, "2099-13"), 0, 50))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void laPaginacionSeAcota() {
        assertThat(reportes.accesos(sinFiltros(), 0, 10_000).tamano()).isEqualTo(200);
        assertThat(reportes.accesos(sinFiltros(), 0, 0).tamano()).isEqualTo(50);
        assertThat(reportes.accesos(sinFiltros(), -5, 10).pagina()).isZero();
    }

    // ------------------------------------------------------------ exportación

    @Test
    void exportarGeneraUnCsvSeguroYQuedaRegistrado() {
        Long idJuan = rolDe(CEDULA_JUAN).getId();
        consulta.verComoPersonal(personal("zz.rrhh", CEDULA_PERSONAL), PERIODO, idJuan, "=HYPERLINK(\"http://x\")", IP);
        long exportacionesAntes = accionRepository.findAll().stream()
                .filter(a -> a.getTipoAccion() == TipoAccion.REPORTE_EXPORTADO).count();

        ArchivoDescargaDto archivo = reportes.exportarAccesos(deUsuario("zz.rrhh"));
        String csv = new String(Base64.getDecoder().decode(archivo.contenidoBase64()), StandardCharsets.UTF_8);

        assertThat(archivo.tipoContenido()).isEqualTo("text/csv");
        assertThat(archivo.nombreArchivo()).startsWith("auditoria-accesos-").endsWith(".csv");
        assertThat(archivo.filas()).isEqualTo(1);
        assertThat(archivo.truncado()).isFalse();
        assertThat(csv).startsWith("\uFEFFFecha;Usuario;Tipo");
        assertThat(csv).contains("'=HYPERLINK");             // la fórmula quedó neutralizada
        assertThat(csv).contains("17******75").doesNotContain(CEDULA_JUAN);
        assertThat(accionRepository.findAll().stream()
                .filter(a -> a.getTipoAccion() == TipoAccion.REPORTE_EXPORTADO).count())
                .isEqualTo(exportacionesAntes + 1);
    }

    // ------------------------------------------------------------ acciones y cobertura

    @Test
    void lasAccionesSeFiltranPorTipoYPeriodo() {
        var publicaciones = reportes.acciones(new FiltroAcciones(null, null, null, TipoAccion.PERIODO_PUBLICADO, "2099-03"), 0, 50);

        assertThat(publicaciones.elementos()).singleElement().satisfies(a -> {
            assertThat(a.tipoAccion()).isEqualTo("PERIODO_PUBLICADO");
            assertThat(a.detalle()).isEqualTo("roles=2, sinRol=0");
        });
    }

    @Test
    void laCoberturaSoloCuentaLaLecturaPropiaPermitida() {
        UsuarioAutenticado juan = empleado("zz.juan", CEDULA_JUAN);
        UsuarioAutenticado maria = empleado("zz.maria", CEDULA_MARIA);
        consulta.consultarPropio(juan, CEDULA_JUAN, null, IP);                    // Juan abre su rol
        intentoFallido(maria, CEDULA_JUAN);                                       // María intenta con la cédula de Juan: denegado
        consulta.verComoPersonal(personal("zz.rrhh", CEDULA_PERSONAL), PERIODO,
                rolDe(CEDULA_MARIA).getId(), "Revisión previa", IP);              // RRHH abre el de María

        CoberturaPeriodoDto cobertura = reportes.cobertura(PERIODO);

        assertThat(cobertura.rolesPublicados()).isEqualTo(2);
        assertThat(cobertura.leidos()).isEqualTo(1);
        assertThat(cobertura.pendientes()).isEqualTo(1);
        assertThat(cobertura.nombresPendientes()).containsExactly(NOMBRE_MARIA);

        consulta.consultarPropio(maria, CEDULA_MARIA, null, IP);                  // ahora María abre el suyo
        assertThat(reportes.cobertura(PERIODO).pendientes()).isZero();
    }

    @Test
    void laCoberturaExigeUnPeriodoExistenteYPublicado() {
        assertThatThrownBy(() -> reportes.cobertura(YearMonth.of(2098, 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        carga.cargar(ABRIL, List.of(archivo(CEDULA_JUAN + ".pdf", rol("ABRIL 2099", NOMBRE_JUAN))));   // cargado, sin publicar
        assertThatThrownBy(() -> reportes.cobertura(ABRIL))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("aún no está publicado");
    }
}
