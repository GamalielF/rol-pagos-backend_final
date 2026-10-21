package ec.fin.coopsanjose.rol_pagos_backend_final.notificacion.config;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * Cómo se arma cada envío a la API (se lee de app.correo.api.*). Sin nada configurado: canal EMAIL,
 * sin plantilla y sin redirección.
 */
public record EnvioCorreoProperties(String canal, String codigoPlantilla, Map<String, String> variables,
                                    String redirigirA) {

    public static final EnvioCorreoProperties SIN_AJUSTES = new EnvioCorreoProperties(null, null, null, null);

    public EnvioCorreoProperties {
        canal = (canal == null || canal.isBlank()) ? "EMAIL" : canal.trim();
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }

    public boolean usaPlantilla() {
        return codigoPlantilla != null && !codigoPlantilla.isBlank();
    }

    /** Pruebas: todo aviso sale a una única dirección. */
    public boolean redirige() {
        return redirigirA != null && !redirigirA.isBlank();
    }

    public static EnvioCorreoProperties desde(Environment entorno) {
        if (entorno instanceof ConfigurableEnvironment configurable) {
            return Binder.get(configurable).bind("app.correo.api", EnvioCorreoProperties.class).orElse(SIN_AJUSTES);
        }
        return SIN_AJUSTES;
    }
}