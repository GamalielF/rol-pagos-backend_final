package ec.fin.coopsanjose.rol_pagos_backend_final.directorio.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Sobre de la respuesta del AD: {status, message, content: [...], timestamp, success}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaAdDto(String status, String message, List<UsuarioAdDto> content, Boolean success) {
}