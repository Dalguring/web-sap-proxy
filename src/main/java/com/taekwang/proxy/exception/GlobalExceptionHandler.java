package com.taekwang.proxy.exception;

import com.taekwang.proxy.model.SimpleProxyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SimpleProxyResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldname = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldname, errorMessage);
        });

        log.error("Validation error: {}", errors);

        SimpleProxyResponse response = SimpleProxyResponse.error(
                "Validation failed: " + errors.toString()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SimpleProxyResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.error("Invalid argument: {}", ex.getMessage());

        SimpleProxyResponse response = SimpleProxyResponse.error(ex.getMessage());

        return ResponseEntity.badRequest().body(response);  // ← 400
    }

    @ExceptionHandler(ProxyException.class)
    public ResponseEntity<SimpleProxyResponse> handleProxyException(ProxyException ex) {
        log.error("Proxy error: {}", ex.getMessage(), ex);

        SimpleProxyResponse response = SimpleProxyResponse.error(ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SimpleProxyResponse> handleException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        SimpleProxyResponse response = SimpleProxyResponse.error(
                "Internal server error: " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
