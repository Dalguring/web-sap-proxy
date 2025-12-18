package com.taekwang.proxy.model;

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
    private String interfaceId;
    private Map<String, Object> data;
    private String requestId;
    private String userId;
}
