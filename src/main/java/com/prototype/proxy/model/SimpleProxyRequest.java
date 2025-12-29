package com.prototype.proxy.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * WEB에서 Proxy 서버로 전송하는 요청 구조
 */

@Schema(
        description = "통합 인터페이스 실행 요청 정보 (SAP RFC 호출)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = """
                {
                  "interfaceId": "WORK_ORDER",
                  "data": {"plants": {"plant": "1110"}},
                  "userId": "test"
                }
                """
)
@Data
@NoArgsConstructor
public class SimpleProxyRequest {

    @NotBlank(message = "Interface ID is required")
    @Schema(description = "실행할 인터페이스 고유 식별자(ID)", example = "STOCK_MOVEMENT")
    private String interfaceId;

    @NotNull(message = "Data must exists")
    @Schema(description = "인터페이스 정의(YAML)에 명시된 구조에 따른 가변 요청 데이터",
            example = "{\"plants\": {\"plant\": \"1110\"}}")
    private Map<String, Object> data;

    @Schema(description = "요청 추적을 위한 고유 ID (생략 시 자동 생성)", example = "REQ-20231229-001")
    private String requestId;

    @Schema(description = "요청 시스템", example = "WMS")
    private String userId;
}
