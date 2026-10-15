package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.NegocioException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ServicioExternoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.util.CedulaValidator;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.DirectorioAdClient;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto.UsuarioAdDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config.AdProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.EstadoDirectorioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.PerfilDirectorioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.ResultadoSincronizacionDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.ResultadoSincronizacion;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.SincronizacionAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.mapper.DirectorioAdMapper;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.mapper.FilaDirectorio;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.SincronizacionAdRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/** Reglas de la sincronización con el AD: protecciones, inactivar (nunca borrar) y bitácora. */
@Slf4j
@Service
public class DirectorioAdService {

    private final DirectorioAdClient cliente;
    private final DirectorioAdRepository directorioRepository;
    private final SincronizacionAdRepository sincronizacionRepository;
    private final DirectorioAdMapper mapper;
    private final AdProperties propiedades;
    private final Clock clock;
    private final TransactionTemplate transaccion;
    private final ReentrantLock candado = new ReentrantLock();

    public DirectorioAdService(DirectorioAdClient cliente,
                               DirectorioAdRepository directorioRepository,
                               SincronizacionAdRepository sincronizacionRepository,
                               DirectorioAdMapper mapper,
                               AdProperties propiedades,
                               Clock clock,
                               PlatformTransactionManager transactionManager) {
        this.cliente = cliente;
        this.directorioRepository = directorioRepository;
        this.sincronizacionRepository = sincronizacionRepository;
        this.mapper = mapper;
        this.propiedades = propiedades;
        this.clock = clock;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------ sincronización completa

    public ResultadoSincronizacionDto sincronizar() {
        if (!candado.tryLock()) {
            throw new ConflictoException("Ya hay una sincronización del directorio en curso");
        }
        try {
            return ejecutar();
        } finally {
            candado.unlock();
        }
    }

    private ResultadoSincronizacionDto ejecutar() {
        LocalDateTime inicio = LocalDateTime.now(clock);
        SincronizacionAd bitacora = new SincronizacionAd();
        bitacora.setInicio(inicio);

        Parseo parseo;
        Conteo conteo;
        try {
            List<UsuarioAdDto> recibidos = cliente.listarUsuarios();   // red: fuera de la transacción
            parseo = parsear(recibidos);
            validar(parseo);                                           // protección ANTES de escribir
            conteo = transaccion.execute(estado -> aplicar(parseo));   // BD: una sola transacción
        } catch (RuntimeException e) {
            registrarFallo(bitacora, e);
            throw e;
        }

        LocalDateTime fin = LocalDateTime.now(clock);
        bitacora.setFin(fin);
        bitacora.setResultado(ResultadoSincronizacion.EXITOSA);
        bitacora.setRecibidos(parseo.recibidos());
        bitacora.setCreados(conteo.creados());
        bitacora.setActualizados(conteo.actualizados());
        bitacora.setSinCambios(conteo.sinCambios());
        bitacora.setInactivados(conteo.inactivados());
        bitacora.setOmitidos(parseo.omitidos());
        bitacora.setMensaje(parseo.cedulasInvalidas() > 0
                ? "Cédulas con dígito verificador inválido: " + parseo.cedulasInvalidas() : null);
        sincronizacionRepository.save(bitacora);

        log.info("Directorio sincronizado: {} recibidos, {} creados, {} actualizados, {} inactivados, {} omitidos",
                parseo.recibidos(), conteo.creados(), conteo.actualizados(), conteo.inactivados(), parseo.omitidos());
        return new ResultadoSincronizacionDto(parseo.recibidos(), conteo.creados(), conteo.actualizados(),
                conteo.sinCambios(), conteo.inactivados(), parseo.omitidos(), parseo.cedulasInvalidas(), inicio, fin);
    }

    private void registrarFallo(SincronizacionAd bitacora, RuntimeException e) {
        bitacora.setFin(LocalDateTime.now(clock));
        bitacora.setResultado(ResultadoSincronizacion.FALLIDA);
        String mensaje = (e instanceof NegocioException) ? e.getMessage() : "Error interno durante la sincronización";
        bitacora.setMensaje(mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje);
        try {
            sincronizacionRepository.save(bitacora);
        } catch (RuntimeException ex) {
            log.error("No se pudo registrar la sincronización fallida", ex);
        }
        log.warn("Sincronización del directorio fallida: {}", mensaje);
    }

    /** Normaliza y filtra los registros del AD. No toca la base de datos. */
    private Parseo parsear(List<UsuarioAdDto> recibidos) {
        Set<String> excluidas = propiedades.cuentasExcluidas().stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Map<String, FilaDirectorio> filas = new LinkedHashMap<>();
        int omitidos = 0;
        int cedulasInvalidas = 0;
        for (UsuarioAdDto dto : recibidos) {
            FilaDirectorio fila = mapper.aFila(dto);
            if (fila == null || excluidas.contains(fila.username()) || filas.containsKey(fila.username())) {
                omitidos++;
                continue;
            }
            if (!CedulaValidator.esValida(fila.cedula())) {
                cedulasInvalidas++;   // se guarda igual: solo se informa
            }
            filas.put(fila.username(), fila);
        }
        return new Parseo(new ArrayList<>(filas.values()), recibidos.size(), omitidos, cedulasInvalidas);
    }

    /** Corta la sincronización si la respuesta parece incompleta: evita desactivar a todos por un error. */
    private void validar(Parseo parseo) {
        if (parseo.filas().isEmpty()) {
            throw new ServicioExternoException("El AD no devolvió usuarios utilizables");
        }
        long activosNuevos = parseo.filas().stream().filter(FilaDirectorio::activo).count();
        long activosPrevios = directorioRepository.countByEstadoRegistro(EstadoRegistro.ACTIVO);
        long minimo = (long) Math.ceil(activosPrevios * propiedades.sincronizacion().porcentajeMinimo() / 100.0);
        if (activosNuevos < minimo) {
            throw new ServicioExternoException("El AD devolvió " + activosNuevos
                    + " usuarios activos y se esperaban al menos " + minimo + ". No se aplicó ningún cambio");
        }
    }

    /** Crea, actualiza e inactiva. Nunca borra. */
    private Conteo aplicar(Parseo parseo) {
        List<DirectorioAd> existentes = directorioRepository.findAll();
        Map<String, DirectorioAd> porUsername = existentes.stream()
                .collect(Collectors.toMap(d -> d.getUsernameAd().toLowerCase(Locale.ROOT), d -> d, (a, b) -> a));
        Set<String> presentes = parseo.filas().stream().map(FilaDirectorio::username).collect(Collectors.toSet());

        int creados = 0;
        int actualizados = 0;
        int sinCambios = 0;
        int inactivados = 0;

        for (FilaDirectorio fila : parseo.filas()) {
            DirectorioAd actual = porUsername.get(fila.username());
            if (actual == null) {
                DirectorioAd nuevo = new DirectorioAd();
                nuevo.setUsernameAd(fila.username());
                mapper.copiar(nuevo, fila);
                directorioRepository.save(nuevo);
                creados++;
            } else if (mapper.difiere(actual, fila)) {
                mapper.copiar(actual, fila);
                directorioRepository.save(actual);
                actualizados++;
            } else {
                sinCambios++;
            }
        }
        for (DirectorioAd d : existentes) {
            boolean ausente = !presentes.contains(d.getUsernameAd().toLowerCase(Locale.ROOT));
            if (ausente && d.getEstadoRegistro() == EstadoRegistro.ACTIVO) {
                d.inactivar();
                directorioRepository.save(d);
                inactivados++;
            }
        }
        return new Conteo(creados, actualizados, sinCambios, inactivados);
    }

    // ------------------------------------------------------------ búsqueda en vivo

    /**
     * Consulta al AD los usuarios de una identificación y actualiza la copia local.
     * NUNCA inactiva a nadie: la respuesta es parcial por definición. Es la base de la resolución
     * "cédula del PDF -> código de usuario" de la carga masiva.
     */
    public List<PerfilDirectorioDto> buscarEnAd(String cedula) {
        List<UsuarioAdDto> recibidos = cliente.buscarPorCodUsuario(cedula);   // red: fuera de la transacción
        Parseo parseo = parsear(recibidos);
        if (parseo.filas().isEmpty()) {
            throw new RecursoNoEncontradoException("El AD no tiene usuarios utilizables para esa identificación");
        }
        List<PerfilDirectorioDto> resultado = transaccion.execute(
                estado -> parseo.filas().stream().map(this::guardarFila).toList());
        return resultado == null ? List.of() : resultado;
    }

    private PerfilDirectorioDto guardarFila(FilaDirectorio fila) {
        DirectorioAd d = directorioRepository.findByUsernameAd(fila.username()).orElseGet(() -> {
            DirectorioAd nuevo = new DirectorioAd();
            nuevo.setUsernameAd(fila.username());
            return nuevo;
        });
        if (d.getId() == null || mapper.difiere(d, fila)) {
            mapper.copiar(d, fila);
            d = directorioRepository.save(d);
        }
        return mapper.aPerfil(d);
    }

    // ------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public EstadoDirectorioDto estado() {
        Optional<SincronizacionAd> ultima = sincronizacionRepository.findFirstByOrderByInicioDesc();
        return new EstadoDirectorioDto(
                directorioRepository.countByEstadoRegistro(EstadoRegistro.ACTIVO),
                directorioRepository.countByEstadoRegistro(EstadoRegistro.INACTIVO),
                ultima.map(SincronizacionAd::getInicio).orElse(null),
                ultima.map(s -> s.getResultado().name()).orElse(null),
                ultima.map(SincronizacionAd::getMensaje).orElse(null));
    }

    @Transactional(readOnly = true)
    public PerfilDirectorioDto buscar(String username) {
        return mapper.aPerfil(directorioRepository.findByUsernameAd(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe en el directorio")));
    }

    private record Parseo(List<FilaDirectorio> filas, int recibidos, int omitidos, int cedulasInvalidas) {
    }

    private record Conteo(int creados, int actualizados, int sinCambios, int inactivados) {
    }
}