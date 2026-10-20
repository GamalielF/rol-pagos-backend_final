package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.dto.response;

/** Un aviso con problema. Solo el nombre: nunca la cédula ni la dirección de correo. */
public record ProblemaNotificacionDto(String nombre, String estado, int intentos, String error) {
}
