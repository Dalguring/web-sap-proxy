package com.prototype.proxy.controller;

import com.prototype.proxy.model.SimpleProxyRequest;
import com.prototype.proxy.model.SimpleProxyResponse;
import com.prototype.proxy.registry.InterfaceRegistry;
import com.prototype.proxy.service.ProxyService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
@Tag(name = "Proxy 관리 API", description = "인터페이스 실행 및 설정 관리")
public class ProxyController {

    private final InterfaceRegistry registry;
    private final ProxyService proxyService;

    /**
     * Proxy 요청 실행
     */
    @Operation(summary = "Proxy 요청 실행", description = "SAP RFC 인터페이스를 실행합니다.")
    @PostMapping(value = "/execute", consumes = "application/json", produces = "application/json")
    public ResponseEntity<SimpleProxyResponse> execute (
        @Validated @RequestBody SimpleProxyRequest request,
        HttpServletRequest servletRequest
    ) {
        request.setIpAddress(servletRequest.getRemoteAddr());
        SimpleProxyResponse response = proxyService.executeRfc(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health Check
     */
    @Operation(summary = "상태 체크", description = "서비스 활성화 여부 및 로드된 인터페이스 수를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<SimpleProxyResponse> health(HttpServletRequest request) {
        SimpleProxyResponse response = proxyService.getHealth(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 등록된 인터페이스 목록 조회
     */
    @Operation(summary = "전체 인터페이스 조회", description = "현재 등록된 모든 인터페이스의 메타데이터를 조회합니다.")
    @GetMapping("/interfaces")
    public ResponseEntity<SimpleProxyResponse> listInterfaces(HttpServletRequest request) {
        SimpleProxyResponse response = proxyService.getInterfaceList(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 인터페이스 조회
     */
    @Operation(summary = "특정 인터페이스 조회", description = "특정 인터페이스의 메타데이터를 조회합니다.")
    @GetMapping("/interfaces/{interfaceId}")
    public ResponseEntity<SimpleProxyResponse> getInterface(@PathVariable String interfaceId, HttpServletRequest request) {
        SimpleProxyResponse response = proxyService.getInterfaceDetail(interfaceId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 인터페이스 정의 재로드 (개발용)
     */
    @PostMapping("/reload")
    @Hidden
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
