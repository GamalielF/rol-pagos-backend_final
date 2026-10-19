package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.DetalleAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.AuditoriaAccesosService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.AccesoDenegadoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioAutenticado;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.RolPagoPdfDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.storage.AlmacenRolesPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Entrega de roles de pago. Cada acceso, permitido o denegado, queda en la bitácora ANTES de entregar nada.
 * No es transaccional a propósito: el registro de un acceso denegado debe guardarse aunque la petición falle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultaRolPagoService {

    private static final String TIPO_CONTENIDO = "application/pdf";

    private final RolPagoRepository rolPagoRepository;
    private final AlmacenRolesPago almacen;
    private final PoliticaAccesoRol politica;
    private final AuditoriaAccesosService auditoria;

    /** El titular consulta SU rol. La búsqueda usa la cédula de la sesión, nunca la que llegó en la petición. */
    public RolPagoPdfDto consultarPropio(UsuarioAutenticado usuario, String cedulaIngresada,
                                         YearMonth periodo, String ip) {
        String periodoTexto = periodo == null ? null : periodo.toString();

        DetalleAcceso decision = politica.evaluarConsultaPropia(usuario, cedulaIngresada);
        if (decision != DetalleAcceso.OK) {
            registrar(TipoAcceso.CONSULTA_PROPIA, ResultadoAcceso.DENEGADO, decision, usuario,
                    cedulaIngresada, periodoTexto, null, null, ip);
            throw new AccesoDenegadoException("No tiene permiso para consultar este rol de pago");
        }

        Optional<RolPago> elegido = publicadosDe(usuario.cedula()).stream()
                .filter(r -> periodo == null || r.getPeriodo().getFechaPeriodo().equals(periodo.atDay(1)))
                .findFirst();
        if (elegido.isEmpty()) {
            registrar(TipoAcceso.CONSULTA_PROPIA, ResultadoAcceso.DENEGADO, DetalleAcceso.ROL_NO_ENCONTRADO,
                    usuario, usuario.cedula(), periodoTexto, null, null, ip);
            throw new RecursoNoEncontradoException("No hay un rol de pago publicado para usted en ese período");
        }

        RolPago rol = elegido.get();
        String periodoDelRol = YearMonth.from(rol.getPeriodo().getFechaPeriodo()).toString();
        byte[] pdf = leerOFallar(rol, TipoAcceso.CONSULTA_PROPIA, usuario, periodoDelRol, ip);
        registrar(TipoAcceso.CONSULTA_PROPIA, ResultadoAcceso.PERMITIDO, DetalleAcceso.OK, usuario,
                usuario.cedula(), periodoDelRol, rol.getId(), null, ip);
        return aDto(rol, pdf);
    }

    /** Períodos publicados en los que la persona de la sesión tiene rol (para el selector de la pantalla). */
    public List<String> listarPeriodosPropios(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.cedula() == null || usuario.cedula().isBlank()) {
            return List.of();
        }
        return publicadosDe(usuario.cedula()).stream()
                .map(r -> YearMonth.from(r.getPeriodo().getFechaPeriodo()).toString())
                .toList();
    }

    /** RRHH/Admin ven el rol de otra persona: motivo obligatorio y queda registrado. Sirve también antes de publicar. */
    public RolPagoPdfDto verComoPersonal(UsuarioAutenticado actor, YearMonth periodo, Long idRol,
                                         String motivo, String ip) {
        String periodoTexto = periodo.toString();

        if (!politica.puedeConsultarAjeno(actor)) {
            registrar(TipoAcceso.CONSULTA_AJENA, ResultadoAcceso.DENEGADO, DetalleAcceso.SIN_PERMISO, actor,
                    null, periodoTexto, idRol, motivo, ip);
            throw new AccesoDenegadoException("No tiene permiso para consultar roles de pago de otras personas");
        }

        Optional<RolPago> encontrado = rolPagoRepository.buscarConPeriodo(idRol)
                .filter(r -> r.getPeriodo().getFechaPeriodo().equals(periodo.atDay(1)));
        if (encontrado.isEmpty()) {
            registrar(TipoAcceso.CONSULTA_AJENA, ResultadoAcceso.DENEGADO, DetalleAcceso.ROL_NO_ENCONTRADO, actor,
                    null, periodoTexto, idRol, motivo, ip);
            throw new RecursoNoEncontradoException("El rol de pago no existe en ese período");
        }

        RolPago rol = encontrado.get();
        TipoAcceso tipo = Objects.equals(actor.cedula(), rol.getCedula())
                ? TipoAcceso.CONSULTA_PROPIA : TipoAcceso.CONSULTA_AJENA;
        byte[] pdf = leerOFallar(rol, tipo, actor, periodoTexto, ip);
        registrar(tipo, ResultadoAcceso.PERMITIDO, DetalleAcceso.OK, actor, rol.getCedula(), periodoTexto,
                rol.getId(), motivo, ip);
        return aDto(rol, pdf);
    }

    // ------------------------------------------------------------ utilidades

    private List<RolPago> publicadosDe(String cedula) {
        return rolPagoRepository.publicadosDe(cedula, EstadoRegistro.ACTIVO, EstadoPeriodo.PUBLICADO);
    }

    private byte[] leerOFallar(RolPago rol, TipoAcceso tipo, UsuarioAutenticado actor, String periodo, String ip) {
        if (!almacen.existe(rol.getRutaArchivo())) {
            log.error("Falta el archivo del rol de pago id={}", rol.getId());
            registrar(tipo, ResultadoAcceso.DENEGADO, DetalleAcceso.ARCHIVO_NO_DISPONIBLE, actor,
                    rol.getCedula(), periodo, rol.getId(), null, ip);
            throw new RecursoNoEncontradoException("El archivo del rol de pago no está disponible");
        }
        return almacen.leer(rol.getRutaArchivo());
    }

    private void registrar(TipoAcceso tipo, ResultadoAcceso resultado, DetalleAcceso detalle,
                           UsuarioAutenticado actor, String cedulaConsultada, String periodo,
                           Long rolPagoId, String motivo, String ip) {
        auditoria.registrar(new AuditoriaAccesosService.Evento(tipo, resultado, detalle,
                actor == null ? "desconocido" : actor.username(), cedulaConsultada, periodo, rolPagoId, motivo, ip));
    }

    private static RolPagoPdfDto aDto(RolPago rol, byte[] pdf) {
        String periodo = YearMonth.from(rol.getPeriodo().getFechaPeriodo()).toString();
        return new RolPagoPdfDto(periodo, "rol-pago-" + periodo + ".pdf", TIPO_CONTENIDO, pdf.length,
                Base64.getEncoder().encodeToString(pdf));
    }
}
