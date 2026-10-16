package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Un usuario tal como lo entrega el AD. NOMBREROLES es el cargo; ESTATUS_USER es 1 (activo) o 0. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioAdDto(
        @JsonProperty("CODIGOUSUARIO") String codigoUsuario,
        @JsonProperty("NOMBRE") String nombre,
        @JsonProperty("IDENTIFICACION") String identificacion,
        @JsonProperty("NOMBREROLES") String nombreRoles,
        @JsonProperty("SECUENCIAOFICINA") Integer secuenciaOficina,
        @JsonProperty("FECHACREACION") String fechaCreacion,
        @JsonProperty("ESTATUS_USER") Integer estatusUser) {
}
