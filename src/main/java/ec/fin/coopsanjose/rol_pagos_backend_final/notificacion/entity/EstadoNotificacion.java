package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.entity;

public enum EstadoNotificacion {
    PENDIENTE,   // por enviar (o esperando un reintento)
    ENVIADA,
    FALLIDA,     // agotó los intentos o el servicio rechazó la dirección
    OMITIDA      // la persona no tiene una cuenta activa con correo
}