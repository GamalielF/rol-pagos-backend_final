package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.mapper;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.entity.EstadoRegistro;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto.UsuarioAdDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.config.AdProperties;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.dto.response.PerfilDirectorioDto;
import ec.fin.coopsanjose.rol_pagos_backend_final.directorio.entity.DirectorioAd;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/** Traduce entre el formato del AD, nuestra entidad y nuestros DTO. Sin reglas de negocio ni acceso a datos. */
@Component
public class DirectorioAdMapper {

    private final AdProperties propiedades;

    public DirectorioAdMapper(AdProperties propiedades) {
        this.propiedades = propiedades;
    }

    /** Normaliza un registro del AD. Devuelve null si no sirve (sin código de usuario o sin identificación válida). */
    public FilaDirectorio aFila(UsuarioAdDto dto) {
        String username = limpiar(dto.codigoUsuario(), 50);
        String cedula = normalizarCedula(dto.identificacion());
        if (username == null || cedula == null) {
            return null;
        }
        return new FilaDirectorio(username.toLowerCase(Locale.ROOT), cedula,
                limpiar(dto.nombre(), 150), limpiar(dto.nombreRoles(), 255),
                dto.secuenciaOficina(), parsearFecha(dto.fechaCreacion()),
                Integer.valueOf(1).equals(dto.estatusUser()));
    }

    /** Copia los datos de la fila a la entidad. ESTATUS_USER = 1 queda ACTIVO; cualquier otro valor, INACTIVO. */
    public void copiar(DirectorioAd destino, FilaDirectorio fila) {
        destino.setCedula(fila.cedula());
        destino.setNombreCompleto(fila.nombre());
        destino.setCargo(fila.cargo());
        destino.setSecuenciaOficina(fila.secuenciaOficina());
        destino.setFechaCreacionAd(fila.fechaCreacion());
        destino.setEmail(correo(fila.username()));
        if (fila.activo()) {
            destino.activar();
        } else {
            destino.inactivar();
        }
    }

    /** ¿Hay algún cambio entre la copia local y lo que dice el AD? Evita escrituras innecesarias. */
    public boolean difiere(DirectorioAd actual, FilaDirectorio fila) {
        EstadoRegistro esperado = fila.activo() ? EstadoRegistro.ACTIVO : EstadoRegistro.INACTIVO;
        return !Objects.equals(actual.getCedula(), fila.cedula())
                || !Objects.equals(actual.getNombreCompleto(), fila.nombre())
                || !Objects.equals(actual.getCargo(), fila.cargo())
                || !Objects.equals(actual.getSecuenciaOficina(), fila.secuenciaOficina())
                || !Objects.equals(actual.getFechaCreacionAd(), fila.fechaCreacion())
                || !Objects.equals(actual.getEmail(), correo(fila.username()))
                || actual.getEstadoRegistro() != esperado;
    }

    /** Vista de un usuario para ADMIN: la cédula va completa (el DTO la oculta de toString). */
    public PerfilDirectorioDto aPerfil(DirectorioAd d) {
        return new PerfilDirectorioDto(d.getUsernameAd(), d.getNombreCompleto(), d.getCargo(), d.getEmail(),
                d.getEstadoRegistro().name(), d.getCedula());
    }

    private String correo(String username) {
        return propiedades.correoDe(username);
    }


    private static String limpiar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }

    private static String normalizarCedula(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() == 9) {
            digitos = "0" + digitos;   // un número guardado como entero pierde el cero inicial
        }
        return digitos.length() == 10 ? digitos : null;
    }

    private static LocalDate parsearFecha(String valor) {
        try {
            return (valor == null || valor.isBlank()) ? null : LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}