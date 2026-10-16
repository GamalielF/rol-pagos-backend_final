package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.storage;

import java.time.YearMonth;

public interface AlmacenRolesPago {

    /** Guarda un PDF nuevo y devuelve su ruta RELATIVA (yyyy-MM/uuid.pdf). */
    String guardar(YearMonth periodo, byte[] contenido);

    /** Reemplaza el contenido de un archivo existente. */
    void reemplazar(String rutaRelativa, byte[] contenido);

    byte[] leer(String rutaRelativa);

    boolean existe(String rutaRelativa);
}
