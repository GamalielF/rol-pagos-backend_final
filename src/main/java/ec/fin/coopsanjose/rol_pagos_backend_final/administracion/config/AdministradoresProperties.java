package ec.fin.coopsanjose.rol_pagos_backend_final.administracion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Locale;

/** Los administradores de arranque. Se normalizan: minúsculas, sin espacios, sin repetidos. */
@ConfigurationProperties(prefix = "app.seguridad")
public record AdministradoresProperties(@DefaultValue List<String> administradoresIniciales) {

    public AdministradoresProperties {
        administradoresIniciales = administradoresIniciales == null ? List.of()
                : administradoresIniciales.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    public boolean esAdministradorInicial(String username) {
        return username != null && administradoresIniciales.contains(username.trim().toLowerCase(Locale.ROOT));
    }
}
