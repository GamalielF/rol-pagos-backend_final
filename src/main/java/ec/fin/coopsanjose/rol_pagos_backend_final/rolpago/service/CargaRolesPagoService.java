package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ServicioExternoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.PeriodoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.util.CedulaValidator;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.config.RolPagoProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.EstadoCarga;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoArchivoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response.ResultadoCargaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.pdf.LectorPdfRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.storage.AlmacenRolesPago;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.AuditoriaAccionesService;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Carga masiva de roles de pago. El archivo se llama <cédula>.pdf; el dueño real lo decide el AD. */
@Slf4j
@Service
public class CargaRolesPagoService {

    private static final Pattern NOMBRE_ARCHIVO = Pattern.compile("^(\\d{10})\\.pdf$", Pattern.CASE_INSENSITIVE);

    private final RolPagoRepository rolPagoRepository;
    private final PeriodoRepository periodoRepository;
    private final ResolutorTitular resolutor;
    private final LectorPdfRolPago lector;
    private final AlmacenRolesPago almacen;
    private final RolPagoProperties propiedades;
    private final TransactionTemplate transaccion;
    private final AuditoriaAccionesService auditoria;

    public CargaRolesPagoService(RolPagoRepository rolPagoRepository,
                                 PeriodoRepository periodoRepository,
                                 ResolutorTitular resolutor,
                                 LectorPdfRolPago lector,
                                 AlmacenRolesPago almacen,
                                 RolPagoProperties propiedades,
                                 PlatformTransactionManager transactionManager,
                                 AuditoriaAccionesService auditoria) {
        this.rolPagoRepository = rolPagoRepository;
        this.periodoRepository = periodoRepository;
        this.resolutor = resolutor;
        this.lector = lector;
        this.almacen = almacen;
        this.propiedades = propiedades;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.auditoria = auditoria;
    }

    public ResultadoCargaDto cargar(YearMonth ym, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new SolicitudInvalidaException("Debe adjuntar al menos un archivo");
        }
        if (archivos.size() > propiedades.maxArchivosPorCarga()) {
            throw new SolicitudInvalidaException("Máximo " + propiedades.maxArchivosPorCarga() + " archivos por carga");
        }
        Periodo periodo = obtenerOCrearPeriodo(ym);
        if (periodo.getEstadoPeriodo() == EstadoPeriodo.PUBLICADO) {
            throw new ConflictoException("El período " + ym + " ya fue publicado y no admite nuevas cargas");
        }

        List<ResultadoArchivoDto> resultados = new ArrayList<>();
        for (MultipartFile archivo : archivos) {
            resultados.add(procesar(ym, periodo.getId(), archivo));
        }
        int cargados = contar(resultados, EstadoCarga.CARGADO);
        int reemplazados = contar(resultados, EstadoCarga.REEMPLAZADO);
        int sinCambios = contar(resultados, EstadoCarga.SIN_CAMBIOS);
        int rechazados = contar(resultados, EstadoCarga.RECHAZADO);
        log.info("Carga del período {}: {} cargados, {} reemplazados, {} sin cambios, {} rechazados",
                ym, cargados, reemplazados, sinCambios, rechazados);
        auditoria.registrar(TipoAccion.CARGA_LOTE, ym.toString(), null,
                "archivos=" + resultados.size() + ", cargados=" + cargados + ", reemplazados=" + reemplazados
                        + ", sinCambios=" + sinCambios + ", rechazados=" + rechazados);
        return new ResultadoCargaDto(ym.toString(), resultados.size(), cargados, reemplazados, sinCambios,
                rechazados, resultados);
    }

    // ------------------------------------------------------------ un archivo

    private ResultadoArchivoDto procesar(YearMonth ym, Long idPeriodo, MultipartFile archivo) {
        String nombre = limpiarNombre(archivo.getOriginalFilename());
        try {
            Matcher m = NOMBRE_ARCHIVO.matcher(nombre);
            if (!m.matches()) {
                return rechazo(nombre, "El nombre debe ser la cédula de 10 dígitos con extensión .pdf");
            }
            String cedula = m.group(1);
            if (!CedulaValidator.esValida(cedula)) {
                return rechazo(nombre, "La cédula del nombre no es válida (dígito verificador)");
            }
            if (archivo.isEmpty()) {
                return rechazo(nombre, "El archivo está vacío");
            }
            if (archivo.getSize() > propiedades.tamanoMaximoArchivo().toBytes()) {
                return rechazo(nombre, "Supera el tamaño máximo de " + propiedades.tamanoMaximoArchivo().toMegabytes() + " MB");
            }

            Optional<byte[]> normalizado = lector.normalizar(archivo.getBytes());
            if (normalizado.isEmpty()) {
                return rechazo(nombre, "El contenido no es un PDF");
            }
            byte[] pdf = normalizado.get();

            LectorPdfRolPago.DatosPdf datos = leerOMarcar(pdf);
            if (datos == null) {
                return rechazo(nombre, "El PDF está dañado, es ilegible o tiene clave");
            }
            if (datos.paginas() > propiedades.maxPaginas()) {
                return rechazo(nombre, "El PDF tiene " + datos.paginas() + " páginas; un rol de pago debe tener "
                        + propiedades.maxPaginas());
            }
            if (datos.periodo().isEmpty()) {
                return rechazo(nombre, "No se encontró el mes y año dentro del PDF");
            }
            if (!datos.periodo().get().equals(ym)) {
                return rechazo(nombre, "El PDF corresponde a " + datos.periodo().get()
                        + " y se está cargando el período " + ym);
            }

            // El titular lo decide el AD, y el nombre impreso en el PDF debe ser el suyo
            List<DirectorioAd> activos = resolutor.activosPorCedula(cedula);
            if (activos.isEmpty()) {
                return rechazo(nombre, resolutor.existeSoloInactivos(cedula)
                        ? "El titular está inactivo en el AD"
                        : "No existe un usuario con esa cédula en el AD");
            }
            boolean nombreCoincide = activos.stream().anyMatch(d -> lector.contieneNombre(datos, d.getNombreCompleto()));
            if (!nombreCoincide) {
                return rechazo(nombre, "El nombre dentro del PDF no coincide con el del titular en el AD");
            }

            String hash = sha256(pdf);
            EstadoCarga estado = Objects.requireNonNull(
                    transaccion.execute(tx -> guardar(ym, idPeriodo, cedula, pdf, hash)));
            return new ResultadoArchivoDto(nombre, estado, mensajeDe(estado));

        } catch (ServicioExternoException e) {
            return rechazo(nombre, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return rechazo(nombre, "Otro usuario cargó este rol al mismo tiempo; vuelva a intentarlo");
        } catch (IOException | RuntimeException e) {
            log.error("Error procesando un archivo de la carga", e);
            return rechazo(nombre, "Error interno al procesar el archivo");
        }
    }

    /** Crea, reemplaza o deja igual. Un reemplazo escribe un archivo NUEVO: el anterior se conserva en disco. */
    private EstadoCarga guardar(YearMonth ym, Long idPeriodo, String cedula, byte[] pdf, String hash) {
        Optional<RolPago> existente = rolPagoRepository.findByPeriodo_IdAndCedula(idPeriodo, cedula);
        if (existente.isPresent()) {
            RolPago rol = existente.get();
            boolean estabaInactivo = rol.getEstadoRegistro() == EstadoRegistro.INACTIVO;
            String hashAnterior = rol.getHashSha256();
            if (!estabaInactivo && hash.equals(hashAnterior)) {
                return EstadoCarga.SIN_CAMBIOS;
            }
            rol.setRutaArchivo(almacen.guardar(ym, pdf));
            rol.setHashSha256(hash);
            rol.setTamanoBytes(pdf.length);
            rol.setMesPdf(ym.atDay(1));
            rol.activar();
            RolPago guardado = rolPagoRepository.save(rol);
            if (estabaInactivo && hash.equals(hashAnterior)) {
                auditoria.registrar(TipoAccion.ROL_ACTIVADO, ym.toString(), guardado.getId(),
                        "Reactivado al volver a cargar el mismo archivo");
            } else {
                auditoria.registrar(TipoAccion.ROL_REEMPLAZADO, ym.toString(), guardado.getId(),
                        "Contenido reemplazado (huella " + corta(hashAnterior) + " -> " + corta(hash) + ")");
            }
            return EstadoCarga.REEMPLAZADO;
        }
        RolPago nuevo = new RolPago();
        nuevo.setPeriodo(periodoRepository.findById(idPeriodo).orElseThrow());
        nuevo.setCedula(cedula);
        nuevo.setRutaArchivo(almacen.guardar(ym, pdf));
        nuevo.setHashSha256(hash);
        nuevo.setTamanoBytes(pdf.length);
        nuevo.setMesPdf(ym.atDay(1));
        rolPagoRepository.save(nuevo);
        return EstadoCarga.CARGADO;
    }

    private static String corta(String hash) {
        return hash == null ? "-" : hash.substring(0, Math.min(8, hash.length()));
    }

    // ------------------------------------------------------------ utilidades

    private Periodo obtenerOCrearPeriodo(YearMonth ym) {
        LocalDate fecha = ym.atDay(1);
        return periodoRepository.findByFechaPeriodo(fecha).orElseGet(() -> {
            try {
                Periodo nuevo = new Periodo();
                nuevo.setFechaPeriodo(fecha);
                return periodoRepository.save(nuevo);
            } catch (DataIntegrityViolationException e) {   // otro usuario lo creó al mismo tiempo
                return periodoRepository.findByFechaPeriodo(fecha).orElseThrow(() -> e);
            }
        });
    }

    private LectorPdfRolPago.DatosPdf leerOMarcar(byte[] pdf) {
        try {
            return lector.leer(pdf);
        } catch (IOException e) {
            return null;
        }
    }

    private static ResultadoArchivoDto rechazo(String archivo, String motivo) {
        return new ResultadoArchivoDto(archivo, EstadoCarga.RECHAZADO, motivo);
    }

    private static String mensajeDe(EstadoCarga estado) {
        return switch (estado) {
            case CARGADO -> "Cargado correctamente";
            case REEMPLAZADO -> "Reemplazado: el contenido cambió";
            case SIN_CAMBIOS -> "Ya existía un archivo idéntico";
            case RECHAZADO -> "Rechazado";
        };
    }

    private static int contar(List<ResultadoArchivoDto> resultados, EstadoCarga estado) {
        return (int) resultados.stream().filter(r -> r.estado() == estado).count();
    }

    /** Algunos navegadores envían la ruta completa; nos quedamos solo con el nombre, acotado. */
    private static String limpiarNombre(String original) {
        if (original == null) {
            return "";
        }
        String n = original.replace("\\", "/");
        n = n.substring(n.lastIndexOf('/') + 1).trim();
        return n.length() > 100 ? n.substring(0, 100) : n;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
