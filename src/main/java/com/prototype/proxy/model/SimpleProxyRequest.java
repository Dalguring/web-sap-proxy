package com.prototype.proxy.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * WEB에서 Proxy 서버로 전송하는 요청 구조
 */
@Data
@NoArgsConstructor
public class SimpleProxyRequest {

    @NotBlank(message = "Interface ID is required")
    @Schema(
            description = "실행할 인터페이스 ID",
            example = "STOCK_MOVEMENT",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String interfaceId;

    @NotBlank(message = "Data must exists")
    @Schema(
            description = "요청 데이터(key/value). 인터페이스 정의서의 webField를 key로 사용",
            example = """
                    {
                      "plant": "1000",
                      "docDate": "2025-12-27",
                      "items": [
                        { "matnr": "A0001", "qty": 2 }
                      ]
                    }
                    """,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Map<String, Object> data;
    private String requestId;
    private String userId;
}
