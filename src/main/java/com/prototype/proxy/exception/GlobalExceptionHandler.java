package com.prototype.proxy.exception;

import com.prototype.proxy.context.RequestContext;
import com.prototype.proxy.model.SimpleProxyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final RequestContext requestContext;

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<SimpleProxyResponse> handleNotFoundException(NotFoundException ex) {
        String requestId = requestContext.getRequestId();
        log.warn("Resource not found. requestId={}, resource={}, message={}", requestId, ex.getResource(), ex.getMessage());

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("interfaceId", ex.getResource());
        errorData.put("errorType", "NOT_FOUND");

        SimpleProxyResponse response = SimpleProxyResponse.error(ex.getMessage(), requestId, errorData);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<SimpleProxyResponse> handleApiNotFoundException(Exception ex) {
        String requestId = requestContext.getRequestId();
        String path = (ex instanceof NoResourceFoundException nrfe) ? nrfe.getResourcePath() : "unknown";

        log.warn("URL path not found. requestId={}, path={}", requestId, path);

        SimpleProxyResponse response = SimpleProxyResponse.error(
                "The requested endpoint was not found.",
                requestId,
                Map.of("path", path, "errorType", "ENDPOINT_NOT_FOUND")
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<SimpleProxyResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        String requestId = requestContext.getRequestId();
        log.warn("Method not allowed. requestId={}, method={}, supported={}",
                requestId, ex.getMethod(), ex.getSupportedHttpMethods());

        SimpleProxyResponse response = SimpleProxyResponse.error(
                "Method not allowed: " + ex.getMethod(),
                requestId,
                Map.of("supportedMethods", ex.getSupportedHttpMethods())
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(InterfaceMappingException.class)
    public ResponseEntity<SimpleProxyResponse> handleInterfaceMappingException(InterfaceMappingException ex) {
        String requestId = requestContext.getRequestId();

        log.warn("Mapping validation failed. requestId={}, interfaceId={}, message={}",
                requestId, ex.getInterfaceId(), ex.getMessage());

        Map<String, Object> data = new HashMap<>();
        data.put("interfaceId", ex.getInterfaceId());
        data.put("errorType", "MAPPING_VALIDATION");

        SimpleProxyResponse response = SimpleProxyResponse.error(
                "Mapping validation failed: " + ex.getMessage(),
                requestId,
                data
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SimpleProxyResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String requestId = requestContext.getRequestId();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Validation failed. requestId={}, errors={}", requestId, fieldErrors);

        Map<String, Object> data = new HashMap<>();
        data.put("errorType", "VALIDATION");
        data.put("fieldErrors", fieldErrors);

        SimpleProxyResponse response = SimpleProxyResponse.error("Validation failed", requestId, data);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SimpleProxyResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        String requestId = requestContext.getRequestId();

        log.warn("Bad request (IllegalArgumentException). requestId={}, message={}",
                requestId, ex.getMessage(), ex);

        Map<String, Object> data = new HashMap<>();
        data.put("errorType", "BAD_REQUEST");
        data.put("exception", "IllegalArgumentException");

        SimpleProxyResponse response = SimpleProxyResponse.error(ex.getMessage(), requestId, data);

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

        return ResponseEntity.internalServerError().body(response);
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

        return ResponseEntity.internalServerError().body(response);
    }
}
