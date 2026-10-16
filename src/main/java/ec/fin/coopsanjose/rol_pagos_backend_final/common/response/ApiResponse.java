package ec.fin.coopsanjose.rol_pagos_backend_final.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** Sobre único de todas las respuestas de la API (éxitos y errores). */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"status", "message", "content", "errors", "timestamp", "success"})
public record ApiResponse<T>(String status, String message, T content, List<String> errors, LocalDateTime timestamp) {

    // Misma zona horaria que el Clock de AuditoriaConfig
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    public static <T> ApiResponse<T> ok(String message) {
        return ok(message, null);
    }

    public static <T> ApiResponse<T> ok(String message, T content) {
        return new ApiResponse<>("200", message, content, null, ahora());
    }

    public static <T> ApiResponse<T> created(String message, T content) {
        return new ApiResponse<>("201", message, content, null, ahora());
    }

    public static <T> ApiResponse<T> error(HttpStatusCode status, String message) {
        return error(status, message, null);
    }

    public static <T> ApiResponse<T> error(HttpStatusCode status, String message, List<String> errors) {
        return new ApiResponse<>(String.valueOf(status.value()), message, null, errors, ahora());
    }

    /** Campo derivado: verdadero si el código HTTP empieza con 2. */
    @JsonProperty("success")
    public boolean isSuccess() {
        return status != null && status.startsWith("2");
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }
}
