package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

/** El PDF viaja en base64. El nombre sugerido no lleva la cédula. */
public record RolPagoPdfDto(String periodo, String nombreArchivo, String tipoContenido,
                            long tamanoBytes, String contenidoBase64) {
}
