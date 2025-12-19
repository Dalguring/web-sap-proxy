package com.prototype.proxy.exception;

import com.prototype.proxy.context.RequestContext;
import com.prototype.proxy.model.SimpleProxyResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final RequestContext requestContext;

    @ExceptionHandler(InterfaceMappingException.class)
    public ResponseEntity<SimpleProxyResponse> handleInterfaceMappingException(InterfaceMappingException ex) {
        String requestId = requestContext.getRequestId();

        log.warn("Mapping validation failed. requestId={}, interfaceId={}, message={}",
                requestId, ex.getInterfaceId(), ex.getMessage());

        SimpleProxyResponse response = SimpleProxyResponse.error(
                "Mapping validation failed: " + ex.getMessage(),
                requestId,
                Map.of("interfaceId", ex.getInterfaceId(), "errorType", "MAPPING_VALIDATION")
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SimpleProxyResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String requestId = requestContext.getRequestId();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Validation failed. requestId={}, errors={}", requestId, fieldErrors);

        SimpleProxyResponse response = SimpleProxyResponse.error(
                "Validation failed",
                requestId,
                Map.of(
                        "errorType", "VALIDATION",
                        "fieldErrors", fieldErrors
                )
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SimpleProxyResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        String requestId = requestContext.getRequestId();

        log.warn("Bad request (IllegalArgumentException). requestId={}, message={}",
                requestId, ex.getMessage(), ex);

        SimpleProxyResponse response = SimpleProxyResponse.error(
                ex.getMessage(),
                requestId,
                Map.of(
                        "errorType", "BAD_REQUEST",
                        "exception", "IllegalArgumentException"
                )
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ProxyException.class)
    public ResponseEntity<SimpleProxyResponse> handleProxyException(ProxyException ex) {
        String requestId = (ex.getRequestId() != null) ? ex.getRequestId() : requestContext.getRequestId();

        log.error("Proxy error: {}", ex.getMessage(), ex);

        SimpleProxyResponse response = SimpleProxyResponse.error(
                ex.getMessage(),
                requestId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SimpleProxyResponse> handleException(Exception ex) {
        String requestId = requestContext.getRequestId();

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        SimpleProxyResponse response = SimpleProxyResponse.error(
                "Internal server error: " + ex.getMessage(),
                requestId,
                Map.of("errorType", "INTERNAL_ERROR")
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
