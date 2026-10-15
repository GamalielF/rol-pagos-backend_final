package ec.fin.coopsanjose.rol_pagos_backend_final.common.seguridad;

public enum Rol {
    ADMIN,
    RRHH,
    EMPLEADO;

    /** Nombre que usa Spring Security: ROLE_ADMIN, ROLE_RRHH, ROLE_EMPLEADO. */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
