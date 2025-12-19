package com.prototype.proxy.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prototype.proxy.model.SimpleProxyRequest;
import com.prototype.proxy.registry.InterfaceDefinition;
import com.prototype.proxy.model.SimpleProxyResponse;
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
    public void logRequest(SimpleProxyRequest request, InterfaceDefinition definition) {
        try {
            String requestDataJson = objectMapper.writeValueAsString(request.getData());

            ProxyLog logEntity = ProxyLog.builder()
                    .requestId(request.getRequestId())
                    .interfaceId(request.getInterfaceId())
                    .rfcFunction(definition.getRfcFunction())
                    .userId(request.getUserId())
                    .requestData(requestDataJson)
                    .success(null)
                    .createdAt(LocalDateTime.now())
                    .build();

            logRepository.save(logEntity);
            log.debug("Request logged: {}", request.getRequestId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request data", e);
        }
    }

    /**
     * 응답 로깅 (성공)
     */
    @Transactional
    public void logResponse(SimpleProxyRequest request, SimpleProxyResponse response, InterfaceDefinition definition) {
        try {
            ProxyLog logEntity = logRepository.findByRequestId(request.getRequestId());

            if (logEntity == null) {
                String requestDataJson = objectMapper.writeValueAsString(request.getData());
                logEntity = ProxyLog.builder()
                        .requestId(request.getRequestId())
                        .interfaceId(request.getInterfaceId())
                        .rfcFunction(definition.getRfcFunction())
                        .userId(request.getUserId())
                        .requestData(requestDataJson)
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            String responseDataJson = objectMapper.writeValueAsString(response.data());

            logEntity.setResponseData(responseDataJson);
            logEntity.setSuccess(response.success());
            logEntity.setExecutionTimeMs(response.executionTimeMs());

            if (!response.success() && response.message() != null) {
                logEntity.setErrorMessage(response.message());
            }

            logRepository.save(logEntity);
            log.debug("Response logged: {}", request.getRequestId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response data", e);
        }
    }

    /**
     * 에러 로깅
     */
    @Transactional
    public void logError(SimpleProxyRequest request, Exception error) {
        try {
            ProxyLog logEntity = logRepository.findByRequestId(request.getRequestId());

            if (logEntity == null) {
                String requestDataJson = objectMapper.writeValueAsString(request.getData());
                logEntity = ProxyLog.builder()
                        .requestId(request.getRequestId())
                        .interfaceId(request.getInterfaceId())
                        .userId(request.getUserId())
                        .requestData(requestDataJson)
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            logEntity.setSuccess(false);
            logEntity.setErrorMessage(error.getMessage());

            logRepository.save(logEntity);
            log.debug("Error logged: {}", request.getRequestId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request data", e);
        }
    }
}
