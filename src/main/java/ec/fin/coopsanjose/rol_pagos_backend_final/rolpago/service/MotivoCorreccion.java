package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;

/**
 * El motivo que RRHH debe escribir para corregir un período YA publicado (reemplazar, agregar, retirar o reponer un rol).
 * Queda en la auditoría. Mismas medidas que el motivo para ver el rol de otra persona: entre 10 y 200 caracteres.
 */
public final class MotivoCorreccion {

    public static final int MINIMO = 10;
    public static final int MAXIMO = 200;

    private MotivoCorreccion() {
    }

    /** Devuelve el motivo con los espacios limpios y acotado, o rechaza la solicitud si es muy corto. */
    public static String validar(String motivo) {
        String limpio = motivo == null ? "" : motivo.replaceAll("\\s+", " ").trim();
        if (limpio.length() < MINIMO) {
            throw new SolicitudInvalidaException("Para corregir un período ya publicado indique el motivo (mínimo "
                    + MINIMO + " caracteres)");
        }
        return limpio.length() > MAXIMO ? limpio.substring(0, MAXIMO) : limpio;
    }
}
