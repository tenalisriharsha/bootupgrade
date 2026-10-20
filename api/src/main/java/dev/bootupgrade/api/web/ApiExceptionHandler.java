package dev.bootupgrade.api.web;

import dev.bootupgrade.api.service.UpgradeRunNotFoundException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ErrorBody(Instant timestamp, int status, String error, String message) {
    }

    @ExceptionHandler(UpgradeRunNotFoundException.class)
    public ResponseEntity<ErrorBody> handleNotFound(UpgradeRunNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody(Instant.now(), 404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody(Instant.now(), 400, "Bad Request", ex.getMessage()));
    }
}
