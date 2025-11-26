package it.bz.sta.lf;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler {

    // 1) Handles all ResponseStatusException you throw in controllers
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getStatusCode().is4xxClientError() ? "bad_request" : "error");
        body.put("status", ex.getStatusCode().value());
        body.put("message", ex.getReason() != null ? ex.getReason() : ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // 2) Legacy/Java style 404 (if someone uses Optional.orElseThrow())
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String,Object>> notFound(NoSuchElementException ex){
        return ResponseEntity.status(404).body(Map.of(
                "error","not_found",
                "message", ex.getMessage() == null ? "resource not found" : ex.getMessage()
        ));
    }

    // 3) DB constraint / FK errors
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,Object>> badRequest(DataIntegrityViolationException ex){
        return ResponseEntity.badRequest().body(Map.of(
                "error","bad_request",
                "message","data integrity violation",
                "detail", ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage()
        ));
    }

    // 4) Null/invalid IDs going into JPA
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String,Object>> invalidId(InvalidDataAccessApiUsageException ex){
        return ResponseEntity.badRequest().body(Map.of(
                "error","bad_request",
                "message","invalid or null id",
                "detail", ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage()
        ));
    }

    // 5) Invalid UUID (or other type) in @PathVariable / @RequestParam
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String,Object>> badUuid(MethodArgumentTypeMismatchException ex){
        return ResponseEntity.badRequest().body(Map.of(
                "error","bad_request",
                "message","invalid value for parameter: " + ex.getName(),
                "value", String.valueOf(ex.getValue())
        ));
    }

    // 6) (Optional) Bean validation for @Valid DTOs, if you add them later
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_error");
        body.put("message", "request validation failed");
        body.put("fieldErrors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage()))
                .toList());
        return ResponseEntity.badRequest().body(body);
    }

    // 7) Catch-all for unexpected errors (e.g. MinIO down, NPE, etc.)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> generic(Exception ex){
        return ResponseEntity.status(500).body(Map.of(
                "error","internal_error",
                "message","unexpected error",
                "detail", ex.getMessage()
        ));
    }
}
