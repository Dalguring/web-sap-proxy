package com.prototype.proxy.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prototype.proxy.model.SimpleProxyRequest;
import com.prototype.proxy.model.SimpleProxyResponse;
import com.prototype.proxy.registry.InterfaceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Proxy 요청/응답 로깅 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {

    private final ProxyExecutionLogRepository proxyLogRepository;
    private final ObjectMapper objectMapper;
    private final SystemAccessLogRepository systemLogRepository;

    /**
     * 요청 로깅
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRequest(SimpleProxyRequest request) {
        this.logRequest(request, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRequest(SimpleProxyRequest request, InterfaceDefinition definition) {
        ProxyExecutionLog logEntity = createExecutionLog(request, definition);
        proxyLogRepository.save(logEntity);
        log.debug("Request proxy execution logged: {}", request.getRequestId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRequest(String requestId, String endpoint, String method, String ipAddress) {
        SystemAccessLog log = createSystemAccessLog(requestId, endpoint, method, ipAddress);
        systemLogRepository.save(log);
    }

    /**
     * 응답 로깅 (성공)
     */
    @Async
    @Transactional
    public void logResponse(SimpleProxyRequest request, SimpleProxyResponse response, InterfaceDefinition definition) {
        ProxyExecutionLog logEntity = getOrCreateExecutionLog(request, definition);

        if (definition != null) {
            logEntity.setRfcFunction(definition.getRfcFunction());
            logEntity.setSapModule(definition.getSapModule());
        }

        logEntity.setResponseData(toJson(response.data()));
        logEntity.setSuccess(response.success());
        logEntity.setExecutionTimeMs(response.executionTimeMs());

        if (!response.success()) {
            logEntity.setErrorMessage(response.message());
        }

        proxyLogRepository.save(logEntity);
    }

    @Async
    @Transactional
    public void logResponse(String requestId, String endpoint, String method, String ipAddress, SimpleProxyResponse response) {
        SystemAccessLog logEntity = getOrCreateSystemAccessLog(requestId, endpoint, method, ipAddress);

        logEntity.setMetadata(toJson(response.data()));
        logEntity.setSuccess(response.success());
        logEntity.setExecutionTimeMs(response.executionTimeMs());

        systemLogRepository.save(logEntity);
    }

    /**
     * 에러 로깅
     */
    @Async
    @Transactional
    public void logError(SimpleProxyRequest request, Exception error) {
        this.logError(request, error, null);
    }

    @Async
    @Transactional
    public void logError(SimpleProxyRequest request, Exception error, InterfaceDefinition definition) {
        ProxyExecutionLog logEntity = getOrCreateExecutionLog(request, definition);

        if (definition != null) {
            if (logEntity.getRfcFunction() == null) {
                logEntity.setRfcFunction(definition.getRfcFunction());
            }
            logEntity.setSapModule(definition.getSapModule());
        }

        logEntity.setSuccess(false);
        logEntity.setErrorMessage(error.getMessage());

        proxyLogRepository.save(logEntity);
    }

    @Async
    @Transactional
    public void logError(String requestId, String endpoint, String method, String ipAddress, Exception error) {
        SystemAccessLog logEntity = getOrCreateSystemAccessLog(requestId, endpoint, method, ipAddress);

        logEntity.setSuccess(false);
        logEntity.setErrorMessage(error.getMessage());

        systemLogRepository.save(logEntity);
    }

    private ProxyExecutionLog getOrCreateExecutionLog(SimpleProxyRequest request, InterfaceDefinition definition) {
        ProxyExecutionLog logEntity = proxyLogRepository.findByRequestId(request.getRequestId());

        if (logEntity == null) {
            log.debug("No existing log found for requestId: {}, creating new one", request.getRequestId());
            return createExecutionLog(request, definition);
        }

        return logEntity;
    }

    private SystemAccessLog getOrCreateSystemAccessLog(String requestId, String endpoint, String method, String ipAddress) {
        SystemAccessLog logEntity = systemLogRepository.findByRequestId(requestId);

        if (logEntity == null) {
            log.debug("No existing log found for requestId: {}, creating new one", requestId);
            return createSystemAccessLog(requestId, endpoint, method, ipAddress);
        }

        return logEntity;
    }

    private ProxyExecutionLog createExecutionLog(SimpleProxyRequest request, InterfaceDefinition definition) {
        return ProxyExecutionLog.builder()
            .requestId(request.getRequestId())
            .interfaceId(request.getInterfaceId())
            .rfcFunction(definition != null ? definition.getRfcFunction() : null)
            .sapModule(definition != null ? definition.getSapModule() : null)
            .userId(request.getUserId())
            .ipAddress(request.getIpAddress())
            .requestData(toJson(request.getData()))
            .createdAt(LocalDateTime.now())
            .build();
    }

    private SystemAccessLog createSystemAccessLog(String requestId, String endpoint, String method, String ipAddress) {
        return SystemAccessLog.builder()
            .requestId(requestId)
            .endpoint(endpoint)
            .ipAddress(ipAddress)
            .method(method)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed", e);
            return "{\"error\": \"Serialization failed\"}";
        }
    }
}
