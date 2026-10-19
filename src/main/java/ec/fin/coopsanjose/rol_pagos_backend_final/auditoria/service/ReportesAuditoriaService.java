package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.request.FiltroAcciones;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.request.FiltroAccesos;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.AccesoAuditoriaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.AccionAuditoriaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.ArchivoDescargaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.CoberturaPeriodoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response.DenegadosPorUsuarioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccionRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.AccesoRolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccionRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccesoRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.EstadoPeriodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.Periodo;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.periodo.PeriodoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.PaginaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.util.CsvUtil;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.util.Enmascarador;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.entity.RolPago;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.repository.RolPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Reportes de auditoría. Ninguno devuelve una cédula completa. */
@Service
@RequiredArgsConstructor
public class ReportesAuditoriaService {

    private static final int TAMANO_POR_DEFECTO = 50;
    private static final int TAMANO_MAXIMO = 200;
    private static final int FILAS_MAXIMAS_EXPORTACION = 20_000;
    private static final long DIAS_MAXIMOS = 366;
    private static final Pattern FORMATO_PERIODO = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccesoRolPagoRepository accesoRepository;
    private final AccionRolPagoRepository accionRepository;
    private final PeriodoRepository periodoRepository;
    private final RolPagoRepository rolPagoRepository;
    private final DirectorioAdRepository directorioRepository;
    private final AuditoriaAccionesService auditoriaAcciones;
    private final Clock clock;

    private record Rango(LocalDateTime desde, LocalDateTime hasta, LocalDate inicio, LocalDate fin) {
    }

    // ------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public PaginaDto<AccesoAuditoriaDto> accesos(FiltroAccesos filtro, int pagina, int tamano) {
        Rango rango = rango(filtro.desde(), filtro.hasta());
        Page<AccesoRolPago> resultado = accesoRepository.buscar(rango.desde(), rango.hasta(), usuario(filtro.usuario()),
                filtro.resultado(), filtro.tipo(), periodo(filtro.periodo()), paginar(pagina, tamano));
        return PaginaDto.de(resultado, this::aDto);
    }

    @Transactional(readOnly = true)
    public PaginaDto<AccionAuditoriaDto> acciones(FiltroAcciones filtro, int pagina, int tamano) {
        Rango rango = rango(filtro.desde(), filtro.hasta());
        Page<AccionRolPago> resultado = accionRepository.buscar(rango.desde(), rango.hasta(), usuario(filtro.usuario()),
                filtro.tipo(), periodo(filtro.periodo()), paginar(pagina, tamano));
        return PaginaDto.de(resultado, this::aDto);
    }

    /** Quién acumuló intentos denegados. Muchas cédulas distintas = alguien está probando números. */
    @Transactional(readOnly = true)
    public List<DenegadosPorUsuarioDto> denegados(LocalDate desde, LocalDate hasta, int minimo) {
        Rango rango = rango(desde, hasta);
        return accesoRepository.denegadosPorUsuario(ResultadoAcceso.DENEGADO, rango.desde(), rango.hasta(), Math.max(minimo, 1))
                .stream()
                .map(d -> new DenegadosPorUsuarioDto(d.getUsuario(), d.getIntentos(), d.getCedulasDistintas(), d.getUltimo()))
                .toList();
    }

    // ------------------------------------------------------------ exportación

    /** CSV de los accesos. Exportar datos personales también queda registrado como acción. */
    @Transactional
    public ArchivoDescargaDto exportarAccesos(FiltroAccesos filtro) {
        Rango rango = rango(filtro.desde(), filtro.hasta());
        String usuario = usuario(filtro.usuario());
        Page<AccesoRolPago> pagina = accesoRepository.buscar(rango.desde(), rango.hasta(), usuario,
                filtro.resultado(), filtro.tipo(), periodo(filtro.periodo()), PageRequest.of(0, FILAS_MAXIMAS_EXPORTACION));

        StringBuilder csv = new StringBuilder("\uFEFF");   // BOM: Excel respeta así las tildes
        csv.append(CsvUtil.fila("Fecha", "Usuario", "Tipo", "Resultado", "Detalle", "Cédula", "Período", "Motivo", "IP"));
        for (AccesoRolPago a : pagina.getContent()) {
            csv.append(CsvUtil.fila(FECHA_HORA.format(a.getCreadoEn()), a.getUsernameAd(), a.getTipoAcceso().name(),
                    a.getResultado().name(), a.getDetalle().name(), Enmascarador.cedula(a.getCedulaConsultada()),
                    a.getPeriodo(), a.getMotivo(), a.getIpOrigen()));
        }
        long filas = pagina.getNumberOfElements();

        auditoriaAcciones.registrar(TipoAccion.REPORTE_EXPORTADO, null, null,
                "accesos: filas=" + filas + ", desde=" + rango.inicio() + ", hasta=" + rango.fin()
                        + (usuario != null ? ", usuario=" + usuario : "")
                        + (filtro.resultado() != null ? ", resultado=" + filtro.resultado() : "")
                        + (filtro.tipo() != null ? ", tipo=" + filtro.tipo() : ""));

        String nombre = "auditoria-accesos-" + LocalDate.now(clock) + ".csv";
        String base64 = Base64.getEncoder().encodeToString(csv.toString().getBytes(StandardCharsets.UTF_8));
        return new ArchivoDescargaDto(nombre, "text/csv", filas, pagina.getTotalElements() > filas, base64);
    }

    // ------------------------------------------------------------ cobertura de lectura

    /** Quién ya abrió su rol y quién no. Solo cuenta la consulta propia permitida. */
    @Transactional(readOnly = true)
    public CoberturaPeriodoDto cobertura(YearMonth ym) {
        Periodo periodo = periodoRepository.findByFechaPeriodo(ym.atDay(1))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el período " + ym));
        if (periodo.getEstadoPeriodo() != EstadoPeriodo.PUBLICADO) {
            throw new ConflictoException("El período " + ym + " aún no está publicado: no hay lecturas que medir");
        }
        List<RolPago> activos = rolPagoRepository.findByPeriodo_IdOrderByCedulaAsc(periodo.getId()).stream()
                .filter(r -> r.getEstadoRegistro() == EstadoRegistro.ACTIVO)
                .toList();
        if (activos.isEmpty()) {
            return new CoberturaPeriodoDto(ym.toString(), 0, 0, 0, List.of());
        }

        Set<Long> leidos = new HashSet<>(accesoRepository.rolesLeidos(
                activos.stream().map(RolPago::getId).toList(), ResultadoAcceso.PERMITIDO, TipoAcceso.CONSULTA_PROPIA));
        List<RolPago> pendientes = activos.stream().filter(r -> !leidos.contains(r.getId())).toList();
        Map<String, String> nombres = nombresPorCedula(pendientes.stream().map(RolPago::getCedula).toList());
        List<String> nombresPendientes = pendientes.stream()
                .map(r -> nombres.getOrDefault(r.getCedula(), "(sin nombre en el directorio)"))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new CoberturaPeriodoDto(ym.toString(), activos.size(), leidos.size(), pendientes.size(), nombresPendientes);
    }

    // ------------------------------------------------------------ utilidades

    private Rango rango(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now(clock);
        LocalDate inicio = desde != null ? desde : fin.minusDays(30);
        if (inicio.isAfter(fin)) {
            throw new SolicitudInvalidaException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > DIAS_MAXIMOS) {
            throw new SolicitudInvalidaException("El rango máximo es de " + DIAS_MAXIMOS + " días");
        }
        return new Rango(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(), inicio, fin);
    }

    private static Pageable paginar(int pagina, int tamano) {
        int tamanoFinal = tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        return PageRequest.of(Math.max(pagina, 0), tamanoFinal);
    }

    private static String usuario(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim().toLowerCase(Locale.ROOT);
        return limpio.length() > 50 ? limpio.substring(0, 50) : limpio;
    }

    private static String periodo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        if (!FORMATO_PERIODO.matcher(valor.trim()).matches()) {
            throw new SolicitudInvalidaException("Período inválido. Formato esperado: yyyy-MM (ej. 2026-08)");
        }
        return valor.trim();
    }

    private Map<String, String> nombresPorCedula(Collection<String> cedulas) {
        Map<String, String> nombres = new HashMap<>();
        if (cedulas.isEmpty()) {
            return nombres;
        }
        directorioRepository.findByCedulaIn(cedulas).stream()
                .sorted(Comparator.comparing((DirectorioAd d) -> d.getEstadoRegistro() != EstadoRegistro.ACTIVO)
                        .thenComparing(DirectorioAd::getUsernameAd))
                .forEach(d -> nombres.putIfAbsent(d.getCedula(), d.getNombreCompleto()));
        return nombres;
    }

    private AccesoAuditoriaDto aDto(AccesoRolPago a) {
        return new AccesoAuditoriaDto(a.getId(), a.getCreadoEn(), a.getUsernameAd(), a.getTipoAcceso().name(),
                a.getResultado().name(), a.getDetalle().name(), Enmascarador.cedula(a.getCedulaConsultada()),
                a.getPeriodo(), a.getMotivo(), a.getIpOrigen());
    }

    private AccionAuditoriaDto aDto(AccionRolPago a) {
        return new AccionAuditoriaDto(a.getId(), a.getCreadoEn(), a.getUsernameAd(), a.getTipoAccion().name(),
                a.getPeriodo(), a.getRolPagoId(), a.getDetalle(), a.getIpOrigen());
    }
}