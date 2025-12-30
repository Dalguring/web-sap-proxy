package com.prototype.proxy.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Proxy 서버에서 WEB으로 반환하는 응답 구조
 */

@Schema(
    description = "통합 인터페이스 응답 표준 모델",
    example = """
        {
          "success": true,
          "message": "null",
          "data": {
            data
          },
          "requestId": "REQ-20251227-001",
          "timestamp": "2025-12-27T18:19:02",
          "executionTimeMs": 45
        }
        """
)
@Builder
public record SimpleProxyResponse (
    @Schema(description = "성공 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean success,

    @Schema(description = "응답 메시지", example = "OK", requiredMode = Schema.RequiredMode.REQUIRED)
    String message,

    @Schema(description = "SAP 결과 데이터 (가변 구조)", requiredMode = Schema.RequiredMode.REQUIRED)
    Map<String, Object> data,

    String requestId,
    LocalDateTime timestamp,
    Long executionTimeMs
) {

    public static SimpleProxyResponse success(Map<String, Object> data, String requestId,
        Long executionTimeMs) {
        return SimpleProxyResponse.builder()
            .success(true)
            .data(data)
            .requestId(requestId)
            .executionTimeMs(executionTimeMs)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static SimpleProxyResponse error(String message, String requestId) {
        return SimpleProxyResponse.builder()
            .success(false)
            .message(message)
            .requestId(requestId)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static SimpleProxyResponse error(String message, String requestId,
        Map<String, Object> data) {
        return SimpleProxyResponse.builder()
            .success(false)
            .message(message)
            .requestId(requestId)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static SimpleProxyResponse error(String message) {
        return error(message, null);
    }
}
