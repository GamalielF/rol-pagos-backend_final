package ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.storage;

import ec.fin.coopsanjose.rol_pagos_backend_final.rolpago.config.RolPagoProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.util.UUID;

@Component
public class AlmacenamientoDisco implements AlmacenRolesPago {

    private final Path base;

    public AlmacenamientoDisco(RolPagoProperties propiedades) throws IOException {
        this.base = Path.of(propiedades.rutaBase()).toAbsolutePath().normalize();
        Files.createDirectories(this.base);
    }

    @Override
    public String guardar(YearMonth periodo, byte[] contenido) {
        // El nombre lo genera el sistema (UUID): nada que venga del usuario forma parte de la ruta
        String relativa = periodo + "/" + UUID.randomUUID() + ".pdf";
        escribir(resolver(relativa), contenido);
        return relativa;
    }

    @Override
    public void reemplazar(String rutaRelativa, byte[] contenido) {
        escribir(resolver(rutaRelativa), contenido);
    }

    @Override
    public byte[] leer(String rutaRelativa) {
        try {
            return Files.readAllBytes(resolver(rutaRelativa));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer un archivo del almacenamiento", e);
        }
    }

    @Override
    public boolean existe(String rutaRelativa) {
        return Files.exists(resolver(rutaRelativa));
    }

    /** Defensa contra path traversal: la ruta final debe quedar DENTRO de la carpeta base. */
    private Path resolver(String rutaRelativa) {
        Path resuelta = base.resolve(rutaRelativa).normalize();
        if (!resuelta.startsWith(base)) {
            throw new IllegalStateException("Ruta fuera del almacenamiento");
        }
        return resuelta;
    }

    /** Escribe a un temporal y lo renombra: si se corta a la mitad, nunca queda un PDF corrupto. */
    private void escribir(Path destino, byte[] contenido) {
        try {
            Files.createDirectories(destino.getParent());
            Path temporal = destino.resolveSibling(UUID.randomUUID() + ".tmp");
            Files.write(temporal, contenido);
            try {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar un archivo en el almacenamiento", e);
        }
    }
}
