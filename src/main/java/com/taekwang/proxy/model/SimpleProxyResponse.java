package com.taekwang.proxy.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Proxy 서버에서 WEB으로 반환하는 응답 구조
 */
@Builder
public record SimpleProxyResponse(
        boolean success,
        String message,
        Map<String, Object> data,
        String requestId,
        LocalDateTime timestamp,
        Long executionTimeMs
) {
    public static SimpleProxyResponse success(Map<String, Object> data) {
        return SimpleProxyResponse.builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SimpleProxyResponse error(String message) {
        return SimpleProxyResponse.builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
