package ec.fin.coopsanjose.rol_pagos_backend_final.common.util;

public final class Enmascarador {

    private Enmascarador() {
    }

    /** 1712345675 -> 17******75. Cualquier otra cosa devuelve null. */
    public static String cedula(String cedula) {
        return (cedula == null || cedula.length() != 10) ? null : cedula.substring(0, 2) + "******" + cedula.substring(8);
    }
}
