package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.dto.response;

/**
 * Resultado de UN archivo del lote. {@code titular} es el nombre que el AD tiene para esa cédula; sirve para que RRHH
 * reconozca a la persona antes de confirmar un reemplazo (es null si el archivo se rechazó antes de llegar a identificarla).
 */
public record ResultadoArchivoDto(String archivo, EstadoCarga estado, String mensaje, String titular) {

    public ResultadoArchivoDto(String archivo, EstadoCarga estado, String mensaje) {
        this(archivo, estado, mensaje, null);
    }
}
