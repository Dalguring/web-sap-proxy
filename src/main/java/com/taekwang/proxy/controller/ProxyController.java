package com.taekwang.proxy.controller;

import com.taekwang.proxy.context.RequestContext;
import com.taekwang.proxy.model.SimpleProxyRequest;
import com.taekwang.proxy.model.SimpleProxyResponse;
import com.taekwang.proxy.registry.InterfaceDefinition;
import com.taekwang.proxy.registry.InterfaceRegistry;
import com.taekwang.proxy.service.ProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
public class ProxyController {
    private final InterfaceRegistry registry;
    private final ProxyService proxyService;
    private final RequestContext requestContext;

    /**
     * Proxy 요청 실행
     */
    @PostMapping("/execute")
    public ResponseEntity<SimpleProxyResponse> execute(@Validated @RequestBody SimpleProxyRequest request) {
        if (request.getRequestId() == null || request.getRequestId().isBlank()) {
            request.setRequestId(UUID.randomUUID().toString());
        }
        
        // RequestId 전역 저장
        requestContext.setRequestId(request.getRequestId());

        log.info("Received proxy request - ID: {}, Interface: {}", request.getRequestId(), request.getInterfaceId());

        SimpleProxyResponse response = proxyService.execute(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Health Check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Interface Proxy Server");
        response.put("timestamp", LocalDateTime.now());
        response.put("loadedInterfaces", registry.getAllDefinitions().size());

        return ResponseEntity.ok(response);
    }

    /**
     * 등록된 인터페이스 목록 조회
     */
    @GetMapping("/interfaces")
    public ResponseEntity<Map<String, Object>> listInterfaces() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Map<String, String>> interfaces = registry.getAllDefinitions()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            Map<String, String> info = new HashMap<>();
                            info.put("name", e.getValue().getName());
                            info.put("description", e.getValue().getDescription());
                            info.put("rfcFunction", e.getValue().getRfcFunction());
                            return info;
                        }
                ));

        response.put("count", interfaces.size());
        response.put("interfaces", interfaces);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 인터페이스 상세 조회
     */
    @GetMapping("/interfaces/{interfaceId}")
    public ResponseEntity<InterfaceDefinition> getInterface(@PathVariable String interfaceId) {
        InterfaceDefinition definition = registry.get(interfaceId);
        return ResponseEntity.ok(definition);
    }

    /**
     * 인터페이스 정의 재로드 (개발용)
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadInterfaces() {
        log.info("Reloading interface definitions...");

        registry.reload();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Interfaces reloaded successfully");
        response.put("count", registry.getAllDefinitions().size());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}
