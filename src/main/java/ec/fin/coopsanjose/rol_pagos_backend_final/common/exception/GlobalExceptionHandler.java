package ec.fin.coopsanjose.rol_pagos_backend_final.common.exception;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import ec.fin.coopsanjose.rol_pagos_backend_final.common.response.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Traduce cualquier excepción a ApiResponse. Nunca expone detalles técnicos al cliente. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- Errores previstos por las reglas del sistema (404, 409...) ----------

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ApiResponse<Void>> alViolarReglaDeNegocio(NegocioException ex) {
        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return responder(ex.getEstado(), ex.getMessage(), null);
    }

    // ---------- Validación de datos de entrada (400) ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> alFallarValidacionDelCuerpo(MethodArgumentNotValidException ex) {
        List<String> errores = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                errores.add(e.getField() + ": " + Objects.toString(e.getDefaultMessage(), "valor inválido")));
        ex.getBindingResult().getGlobalErrors().forEach(e ->
                errores.add(Objects.toString(e.getDefaultMessage(), "valor inválido")));
        errores.sort(String::compareTo);   // orden estable: respuestas predecibles
        log.warn("Validación fallida: {}", errores);
        return responder(HttpStatus.BAD_REQUEST, "Error de validación", errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> alFallarRestricciones(ConstraintViolationException ex) {
        List<String> errores = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
        log.warn("Restricciones violadas: {}", errores);
        return responder(HttpStatus.BAD_REQUEST, "Error de validación", errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> alFallarValidacionDeParametros(HandlerMethodValidationException ex) {
        log.warn("Validación de parámetros fallida");
        return responder(HttpStatus.BAD_REQUEST, "Los parámetros de la petición no son válidos", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> alLeerCuerpoInvalido(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo ilegible: {}", ex.getClass().getSimpleName());
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> alRecibirTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        log.warn("Tipo incorrecto en el parámetro '{}'", ex.getName());
        return responder(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un formato inválido", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> alFaltarParametro(MissingServletRequestParameterException ex) {
        log.warn("Falta el parámetro '{}'", ex.getParameterName());
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", null);
    }

    // ---------- Petición mal dirigida (404, 405, 415) ----------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> alNoExistirLaRuta(NoResourceFoundException ex) {
        log.warn("Ruta inexistente: {}", ex.getResourcePath());
        return responder(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> alUsarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        log.warn("Método no soportado: {}", ex.getMethod());
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "El método " + ex.getMethod() + " no está permitido en esta ruta", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> alUsarTipoDeContenidoNoSoportado(HttpMediaTypeNotSupportedException ex) {
        log.warn("Tipo de contenido no soportado: {}", ex.getContentType());
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El tipo de contenido enviado no está soportado", null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> alLanzarResponseStatus(ResponseStatusException ex) {
        String mensaje = ex.getReason() != null ? ex.getReason() : "La petición no pudo ser procesada";
        log.warn("ResponseStatusException {}: {}", ex.getStatusCode().value(), mensaje);
        return responder(ex.getStatusCode(), mensaje, null);
    }

    // ---------- Base de datos (409) ----------

    /** Bloqueo optimista: alguien modificó el registro mientras tú lo editabas (@Version). */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse<Void>> alHaberConflictoDeVersion(Exception ex) {
        log.warn("Conflicto de versión: {}", ex.getClass().getSimpleName());
        return responder(HttpStatus.CONFLICT,
                "El registro fue modificado por otro usuario. Recargue e intente de nuevo", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> alViolarIntegridad(DataIntegrityViolationException ex) {
        // El detalle (SQL, nombre de restricción) solo va al log, nunca al cliente
        log.warn("Integridad de datos violada: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La operación viola una restricción de integridad de datos", null);
    }
    // ---------- Seguridad dentro de los controladores (401, 403) ----------

    /** Reglas por método (@PreAuthorize). Toda ruta exige sesión, así que aquí siempre hay usuario: 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> alDenegarAcceso(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return responder(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> alFaltarAutenticacion(AuthenticationException ex) {
        log.warn("Autenticación requerida: {}", ex.getClass().getSimpleName());
        return responder(HttpStatus.UNAUTHORIZED, "Debe autenticarse para acceder a este recurso", null);
    }

    // ---------- Red de seguridad ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> alOcurrirErrorInesperado(Exception ex) {
        // Otras excepciones de Spring MVC que ya traen su código 4xx: no deben volverse 500
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            log.warn("{} -> {}", ex.getClass().getSimpleName(), errorResponse.getStatusCode().value());
            return responder(errorResponse.getStatusCode(), "La petición no pudo ser procesada", null);
        }
        String referencia = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Error inesperado [ref={}]", referencia, ex);   // stacktrace completo solo en el log
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor. Referencia: " + referencia, null);
    }

    private ResponseEntity<ApiResponse<Void>> responder(HttpStatusCode estado, String mensaje, List<String> errores) {
        return ResponseEntity.status(estado).body(ApiResponse.error(estado, mensaje, errores));
    }
}
