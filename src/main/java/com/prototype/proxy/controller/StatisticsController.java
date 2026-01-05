package com.prototype.proxy.controller;

import com.prototype.proxy.model.SimpleProxyResponse;
import com.prototype.proxy.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/stats")
@Tag(name = "Statistics API", description = "로그 통계 및 조회")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "일별 모듈 통계 조회")
    @GetMapping("/daily")
    public ResponseEntity<SimpleProxyResponse> getDailyStats(
        @Parameter(description = "조회할 날짜", example = "2025-01-01")
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date
    ) {
        SimpleProxyResponse response = statisticsService.getDailyModuleStats(date);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "모듈 상세(인터페이스별) 통계 조회")
    @GetMapping("/module")
    public ResponseEntity<SimpleProxyResponse> getModuleDetails(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam String module
    ) {
        SimpleProxyResponse response = statisticsService.getModuleDetailStats(date, module);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "에러 로그 상세 조회")
    @GetMapping("/errors")
    public ResponseEntity<SimpleProxyResponse> getErrorLogs(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam String interfaceId
    ) {
        SimpleProxyResponse response = statisticsService.getErrorLogs(date, interfaceId);
        return ResponseEntity.ok(response);
    }
}
