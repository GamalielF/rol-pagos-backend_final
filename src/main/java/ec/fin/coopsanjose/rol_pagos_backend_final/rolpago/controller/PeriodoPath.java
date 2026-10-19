package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.controller;

import ec.fin.coopsanjose.rol_pagos_backend_final.common.exception.SolicitudInvalidaException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

final class PeriodoPath {

    private PeriodoPath() {
    }

    static YearMonth parsear(String periodo) {
        try {
            return YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            throw new SolicitudInvalidaException("Período inválido. Formato esperado: yyyy-MM (ej. 2026-08)");
        }
    }
}