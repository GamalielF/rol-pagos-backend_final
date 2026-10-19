package ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.dto.response;

/** Un archivo generado por el servidor, en base64 como el PDF. truncado = hay más filas que el tope. */
public record ArchivoDescargaDto(String nombreArchivo, String tipoContenido, long filas, boolean truncado,
                                 String contenidoBase64) {
}