package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.config.AdministradoresProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response.RolAsignadoDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.dto.response.UsuarioAdminDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.entity.AsignacionRol;
import ec.fin.coopsanjose.rol_pagos_backend_final.administracion.repository.AsignacionRolRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.TipoAccion;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.service.AuditoriaAccionesService;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.ConflictoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.RecursoNoEncontradoException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.PaginaDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.Rol;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad.UsuarioActual;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.repository.DirectorioAdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Lo que hace el Admin con los roles de los usuarios. Cada cambio queda en la auditoría. */
@Service
@RequiredArgsConstructor
public class AdministracionUsuariosService {

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{1,50}");
    private static final int TAMANO_POR_DEFECTO = 50;
    private static final int TAMANO_MAXIMO = 200;

    private final DirectorioAdRepository directorioRepository;
    private final AsignacionRolRepository asignacionRepository;
    private final AdministradoresProperties administradores;
    private final AuditoriaAccionesService auditoria;

    // ------------------------------------------------------------ consultas

    /** Usuarios ACTIVOS del directorio, con sus roles. Busca por nombre o código de usuario. */
    @Transactional(readOnly = true)
    public PaginaDto<UsuarioAdminDto> buscar(String texto, int pagina, int tamano) {
        Page<DirectorioAd> resultado = directorioRepository.buscar(EstadoRegistro.ACTIVO, patron(texto),
                PageRequest.of(Math.max(pagina, 0), tamano < 1 ? TAMANO_POR_DEFECTO : Math.min(tamano, TAMANO_MAXIMO)));
        List<String> usernames = resultado.getContent().stream().map(DirectorioAd::getUsernameAd).toList();
        Map<String, List<AsignacionRol>> asignaciones = usernames.isEmpty() ? Map.of()
                : asignacionRepository.findByUsernameAdInAndEstadoRegistro(usernames, EstadoRegistro.ACTIVO).stream()
                .collect(Collectors.groupingBy(AsignacionRol::getUsernameAd));
        return PaginaDto.de(resultado, d -> aDto(d.getUsernameAd(), d, asignaciones.getOrDefault(d.getUsernameAd(), List.of())));
    }

    /** Todos los que tienen ADMIN o RRHH, incluidos los administradores de arranque y los dados de baja. */
    @Transactional(readOnly = true)
    public List<UsuarioAdminDto> privilegiados() {
        Set<String> usernames = new TreeSet<>(administradores.administradoresIniciales());
        List<AsignacionRol> activas = asignacionRepository.findByEstadoRegistro(EstadoRegistro.ACTIVO);
        activas.forEach(a -> usernames.add(a.getUsernameAd()));
        if (usernames.isEmpty()) {
            return List.of();
        }
        Map<String, DirectorioAd> directorio = directorioRepository.findByUsernameAdIn(usernames).stream()
                .collect(Collectors.toMap(DirectorioAd::getUsernameAd, Function.identity(), (a, b) -> a));
        Map<String, List<AsignacionRol>> porUsuario = activas.stream()
                .collect(Collectors.groupingBy(AsignacionRol::getUsernameAd));
        return usernames.stream()
                .map(u -> aDto(u, directorio.get(u), porUsuario.getOrDefault(u, List.of())))
                .sorted(Comparator.comparing((UsuarioAdminDto u) -> u.nombre() == null ? "\uFFFF" : u.nombre(),
                        String.CASE_INSENSITIVE_ORDER).thenComparing(UsuarioAdminDto::username))
                .toList();
    }

    // ------------------------------------------------------------ cambios

    /** Idempotente. verRolesAjenos solo afecta a RRHH (el Admin siempre puede). Por defecto, true. */
    @Transactional
    public UsuarioAdminDto otorgar(String usernameSolicitado, Rol rol, Boolean verRolesAjenos) {
        exigirRolAsignable(rol);
        String username = normalizar(usernameSolicitado);
        DirectorioAd usuario = directorioRepository.findByUsernameAd(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe en el directorio"));
        if (usuario.getEstadoRegistro() != EstadoRegistro.ACTIVO) {
            throw new ConflictoException("El usuario está inactivo en el AD: no se le puede asignar un rol");
        }
        if (rol == Rol.ADMIN && administradores.esAdministradorInicial(username)) {
            throw new ConflictoException("Ya es administrador de arranque (se define en la configuración)");
        }

        boolean ver = rol == Rol.ADMIN || verRolesAjenos == null || verRolesAjenos;
        Optional<AsignacionRol> existente = asignacionRepository.findByUsernameAdAndRol(username, rol);
        AsignacionRol asignacion = existente.orElseGet(() -> {
            AsignacionRol nueva = new AsignacionRol();
            nueva.setUsernameAd(username);
            nueva.setRol(rol);
            return nueva;
        });
        boolean cambia = existente.isEmpty() || asignacion.getEstadoRegistro() != EstadoRegistro.ACTIVO
                || asignacion.puedeVerRolesAjenos() != ver;
        if (cambia) {
            asignacion.definirVerRolesAjenos(ver);
            asignacion.activar();
            asignacionRepository.save(asignacion);
            auditoria.registrar(TipoAccion.USUARIO_ROL_OTORGADO, null, null,
                    "usuario=" + username + ", rol=" + rol + (rol == Rol.RRHH ? ", verRolesAjenos=" + ver : ""));
        }
        return aDto(username, usuario, asignacionRepository.findByUsernameAdAndEstadoRegistro(username, EstadoRegistro.ACTIVO));
    }

    @Transactional
    public UsuarioAdminDto revocar(String usernameSolicitado, Rol rol) {
        exigirRolAsignable(rol);
        String username = normalizar(usernameSolicitado);
        if (rol == Rol.ADMIN && administradores.esAdministradorInicial(username)) {
            throw new ConflictoException("Es administrador de arranque: solo se puede quitar desde la configuración");
        }
        if (rol == Rol.ADMIN && UsuarioActual.obtener().map(u -> u.username().equalsIgnoreCase(username)).orElse(false)) {
            throw new ConflictoException("No puede quitarse a sí mismo el rol ADMIN");
        }
        AsignacionRol asignacion = asignacionRepository.findByUsernameAdAndRol(username, rol)
                .filter(a -> a.getEstadoRegistro() == EstadoRegistro.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no tiene ese rol asignado"));
        asignacion.inactivar();
        asignacionRepository.save(asignacion);
        auditoria.registrar(TipoAccion.USUARIO_ROL_REVOCADO, null, null, "usuario=" + username + ", rol=" + rol);
        return aDto(username, directorioRepository.findByUsernameAd(username).orElse(null),
                asignacionRepository.findByUsernameAdAndEstadoRegistro(username, EstadoRegistro.ACTIVO));
    }

    // ------------------------------------------------------------ utilidades

    private static void exigirRolAsignable(Rol rol) {
        if (rol != Rol.ADMIN && rol != Rol.RRHH) {
            throw new SolicitudInvalidaException("Solo se asignan los roles ADMIN y RRHH: EMPLEADO lo es todo usuario activo");
        }
    }

    private static String normalizar(String username) {
        String limpio = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(limpio).matches()) {
            throw new SolicitudInvalidaException("Código de usuario inválido");
        }
        return limpio;
    }

    /** Minúsculas y sin los comodines de LIKE que el usuario pudiera teclear. */
    private static String patron(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().toLowerCase(Locale.ROOT).replaceAll("[%_\\\\]", "");
        if (limpio.isEmpty()) {
            return null;
        }
        return "%" + (limpio.length() > 50 ? limpio.substring(0, 50) : limpio) + "%";
    }

    private UsuarioAdminDto aDto(String username, DirectorioAd directorio, List<AsignacionRol> asignaciones) {
        boolean inicial = administradores.esAdministradorInicial(username);
        List<RolAsignadoDto> roles = new ArrayList<>();
        if (inicial) {
            roles.add(new RolAsignadoDto(Rol.ADMIN.name(), "CONFIGURACION", true));
        }
        asignaciones.stream()
                .filter(a -> !(inicial && a.getRol() == Rol.ADMIN))
                .sorted(Comparator.comparing(a -> a.getRol().ordinal()))
                .forEach(a -> roles.add(new RolAsignadoDto(a.getRol().name(), "ASIGNADO",
                        a.getRol() == Rol.ADMIN || a.puedeVerRolesAjenos())));
        return new UsuarioAdminDto(username,
                directorio == null ? null : directorio.getNombreCompleto(),
                directorio == null ? null : directorio.getCargo(),
                directorio == null ? "NO_EXISTE" : directorio.getEstadoRegistro().name(),
                roles);
    }
}
