package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.service;

import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.DetalleAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.entity.ResultadoAcceso;
import ec.fin.coopsanjose.rol_pagos_backend_final.auditoria.repository.AccesoRolPagoRepository;
import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.config.LimiteIntentosProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Frena a quien prueba cédulas ajenas. Solo cuentan los intentos con una cédula que NO es la suya;
 * los propios intentos bloqueados no extienden el bloqueo (si no, quien insiste no saldría nunca).
 */
@Component
@RequiredArgsConstructor
public class LimiteIntentosConsulta {

    private final AccesoRolPagoRepository accesos;
    private final LimiteIntentosProperties propiedades;
    private final Clock clock;

    public boolean estaBloqueado(String username) {
        LocalDateTime desde = LocalDateTime.now(clock).minus(propiedades.ventana());
        long fallidos = accesos.countByUsernameAdAndResultadoAndDetalleAndCreadoEnAfter(
                username, ResultadoAcceso.DENEGADO, DetalleAcceso.CEDULA_NO_COINCIDE, desde);
        return fallidos >= propiedades.maximo();
    }
}