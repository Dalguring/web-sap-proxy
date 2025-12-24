package com.prototype.proxy.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prototype.proxy.model.SimpleProxyRequest;
import com.prototype.proxy.model.SimpleProxyResponse;
import com.prototype.proxy.registry.InterfaceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Proxy 요청/응답 로깅 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {
    private final ProxyLogRepository logRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logRequest(SimpleProxyRequest request) {
        this.logRequest(request, null);
    }

    @Transactional
    public void logRequest(SimpleProxyRequest request, InterfaceDefinition definition) {
        ProxyLog logEntity = createInitialLog(request, definition);
        logRepository.save(logEntity);
        log.debug("Request logged: {}", request.getRequestId());
    }

    /**
     * 응답 로깅 (성공)
     */
    @Transactional
    public void logResponse(SimpleProxyRequest request, SimpleProxyResponse response, InterfaceDefinition definition) {
        ProxyLog logEntity = getOrCreateLog(request, definition);

        if (definition != null) {
            logEntity.setRfcFunction(definition.getRfcFunction());
        }

        logEntity.setResponseData(toJson(response.data()));
        logEntity.setSuccess(response.success());
        logEntity.setExecutionTimeMs(response.executionTimeMs());

        if (!response.success()) {
            logEntity.setErrorMessage(response.message());
        }

        logRepository.save(logEntity);
    }

    /**
     * 에러 로깅
     */
    @Transactional
    public void logError(SimpleProxyRequest request, Exception error) {
        ProxyLog logEntity = getOrCreateLog(request, null);

        logEntity.setSuccess(false);
        logEntity.setErrorMessage(error.getMessage());

        logRepository.save(logEntity);
    }

    private ProxyLog getOrCreateLog(SimpleProxyRequest request, InterfaceDefinition definition) {
        ProxyLog logEntity = logRepository.findByRequestId(request.getRequestId());

        if (logEntity == null) {
            log.debug("No existing log found for requestId: {}, creating new one", request.getRequestId());
            return createInitialLog(request, definition);
        }

        return logEntity;
    }

    private ProxyLog createInitialLog(SimpleProxyRequest request, InterfaceDefinition definition) {
        return ProxyLog.builder()
                .requestId(request.getRequestId())
                .interfaceId(request.getInterfaceId())
                .rfcFunction(definition != null ? definition.getRfcFunction() : null)
                .userId(request.getUserId())
                .requestData(toJson(request.getData()))
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
