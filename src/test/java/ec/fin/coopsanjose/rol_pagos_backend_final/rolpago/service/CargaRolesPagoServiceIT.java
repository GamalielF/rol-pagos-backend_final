package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.SoporteRolesPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.EstadoCarga;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoArchivoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoCargaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.PdfDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reglas de la carga masiva. El AD "no sabe" de nadie que no esté en el directorio de la prueba (el cliente
 * devuelve vacío); el camino con el AD real lo cubre CargaRolesPagoRealIT.
 */
class CargaRolesPagoServiceIT extends SoporteRolesPago {

    @MockitoBean private DirectorioAdClient cliente;
    @Autowired private GestionRolesPagoService gestion;

    @BeforeEach
    void titularDePrueba() {
        persona("jperez", CEDULA_JUAN, NOMBRE_JUAN, true);
    }

    private void assertRechazo(String nombreArchivo, byte[] contenido, String fragmentoDelMotivo) {
        ResultadoArchivoDto r = cargarUno(nombreArchivo, contenido);

        assertThat(r.estado()).isEqualTo(EstadoCarga.RECHAZADO);
        assertThat(r.mensaje()).contains(fragmentoDelMotivo);
    }

    // ------------------------------------------------------------ lo que sí se carga

    @Test
    void cargaUnRolValido() {
        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        assertThat(r.estado()).isEqualTo(EstadoCarga.CARGADO);
        RolPago guardado = rolDe(CEDULA_JUAN);
        assertThat(guardado.getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(guardado.getHashSha256()).hasSize(64);
        assertThat(guardado.getRutaArchivo()).startsWith("2099-03/").doesNotContain(CEDULA_JUAN);
        assertThat(almacen.existe(guardado.getRutaArchivo())).isTrue();
    }

    @Test
    void elMismoArchivoDosVecesNoCambiaNada() {
        byte[] pdf = rol(MES_PERIODO, NOMBRE_JUAN);   // se genera UNA vez: PDFBox escribe un /ID distinto en cada generación
        cargarUno(CEDULA_JUAN + ".pdf", pdf);
        String rutaInicial = rolDe(CEDULA_JUAN).getRutaArchivo();

        ResultadoArchivoDto segunda = cargarUno(CEDULA_JUAN + ".pdf", pdf);

        assertThat(segunda.estado()).isEqualTo(EstadoCarga.SIN_CAMBIOS);
        assertThat(rolDe(CEDULA_JUAN).getRutaArchivo()).isEqualTo(rutaInicial);
    }

    @Test
    void unContenidoDistintoReemplazaYConservaElArchivoAnterior() {
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
        String rutaAnterior = rolDe(CEDULA_JUAN).getRutaArchivo();
        byte[] corregido = PdfDePrueba.crear("LIQUIDACION DE SUELDO", "MES AÑO", MES_PERIODO, NOMBRE_JUAN, "BONO 10,00");

        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", corregido);

        assertThat(r.estado()).isEqualTo(EstadoCarga.REEMPLAZADO);
        String rutaNueva = rolDe(CEDULA_JUAN).getRutaArchivo();
        assertThat(rutaNueva).isNotEqualTo(rutaAnterior);
        assertThat(almacen.existe(rutaAnterior)).isTrue();   // el anterior queda en disco
    }

    @Test
    void unRolInactivoSeReactivaAlVolverACargarlo() {
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
        gestion.inactivar(PERIODO, rolDe(CEDULA_JUAN).getId());
        assertThat(rolDe(CEDULA_JUAN).getEstadoRegistro()).isEqualTo(EstadoRegistro.INACTIVO);

        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        assertThat(r.estado()).isEqualTo(EstadoCarga.REEMPLAZADO);
        assertThat(rolDe(CEDULA_JUAN).getEstadoRegistro()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    // ------------------------------------------------------------ los 12 motivos de rechazo

    @Test
    void nombreDeArchivoInvalido() {
        assertRechazo("rol_agosto.pdf", rol(MES_PERIODO, NOMBRE_JUAN), "cédula de 10 dígitos");
    }

    @Test
    void cedulaConDigitoVerificadorInvalido() {
        assertRechazo("1710034066.pdf", rol(MES_PERIODO, NOMBRE_JUAN), "dígito verificador");
    }

    @Test
    void archivoVacio() {
        assertRechazo(CEDULA_JUAN + ".pdf", new byte[0], "vacío");
    }

    @Test
    void archivoQueNoEsPdf() {
        assertRechazo(CEDULA_JUAN + ".pdf", "esto no es un pdf".getBytes(), "no es un PDF");
    }

    @Test
    void pdfDanado() {
        assertRechazo(CEDULA_JUAN + ".pdf", "%PDF-1.4\nesto esta roto".getBytes(), "dañado");
    }

    @Test
    void pdfDeOtroMes() {
        assertRechazo(CEDULA_JUAN + ".pdf", rol("JULIO 2099", NOMBRE_JUAN), "2099-07");
    }

    @Test
    void pdfSinMes() {
        assertRechazo(CEDULA_JUAN + ".pdf", PdfDePrueba.crear(NOMBRE_JUAN), "mes y año");
    }

    @Test
    void pdfDeVariasPaginas() {
        assertRechazo(CEDULA_JUAN + ".pdf", PdfDePrueba.crear(2, "MES AÑO", MES_PERIODO, NOMBRE_JUAN), "2 páginas");
    }

    @Test
    void nombreDelPdfQueNoCoincideConElTitular() {
        assertRechazo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, "RUIZ SOTO ANA LUCIA"), "no coincide");
    }

    @Test
    void archivoMuyGrande() {
        assertRechazo(CEDULA_JUAN + ".pdf", new byte[5 * 1024 * 1024 + 1], "tamaño máximo");
    }

    @Test
    void titularInactivoEnElAd() {
        persona("zinactivo", CEDULA_MARIA, NOMBRE_MARIA, false);

        assertRechazo(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA), "inactivo");
    }

    @Test
    void cedulaSinUsuarioEnElAd() {
        assertRechazo("1723456784.pdf", rol(MES_PERIODO, "SILVA TORRES LUIS ALBERTO"), "No existe un usuario");
    }

    // ------------------------------------------------------------ lotes y varios códigos

    @Test
    void unLoteMixtoNoSeDetieneConElPrimerError() {
        ResultadoCargaDto resultado = carga.cargar(PERIODO, List.of(
                archivo(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo("basura.pdf", rol(MES_PERIODO, NOMBRE_JUAN)),
                archivo(CEDULA_MARIA + ".pdf", rol("JULIO 2099", NOMBRE_MARIA))));

        assertThat(resultado.total()).isEqualTo(3);
        assertThat(resultado.cargados()).isEqualTo(1);
        assertThat(resultado.rechazados()).isEqualTo(2);
        assertThat(resultado.resultados()).extracting(ResultadoArchivoDto::estado)
                .containsExactly(EstadoCarga.CARGADO, EstadoCarga.RECHAZADO, EstadoCarga.RECHAZADO);
    }

    @Test
    void variosCodigosDeLaMismaPersonaNoSonAmbiguos() {
        persona("jperez.prueba", CEDULA_JUAN, NOMBRE_JUAN, true);

        ResultadoArchivoDto r = cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));

        assertThat(r.estado()).isEqualTo(EstadoCarga.CARGADO);
        assertThat(rolPagoRepository.findByPeriodo_IdOrderByCedulaAsc(rolDe(CEDULA_JUAN).getPeriodo().getId())).hasSize(1);
    }

    @Test
    void unaCuentaInactivaNoImpideLaCargaSiHayOtraActiva() {
        persona("jperez.antiguo", CEDULA_JUAN, "OTRO NOMBRE ANTIGUO", false);

        assertThat(cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN)).estado())
                .isEqualTo(EstadoCarga.CARGADO);
    }

    @Test
    void unPeriodoPublicadoNoAdmiteCargas() {
        cargarUno(CEDULA_JUAN + ".pdf", rol(MES_PERIODO, NOMBRE_JUAN));
        Periodo periodo = periodoRepository.findByFechaPeriodo(PERIODO.atDay(1)).orElseThrow();
        periodo.setEstadoPeriodo(EstadoPeriodo.PUBLICADO);
        periodoRepository.save(periodo);
        em.flush();

        assertThatThrownBy(() -> cargarUno(CEDULA_MARIA + ".pdf", rol(MES_PERIODO, NOMBRE_MARIA)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("ya fue publicado");
    }
}
