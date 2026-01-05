package com.prototype.proxy.service;

import com.prototype.proxy.dto.InterfaceStatsDto;
import com.prototype.proxy.dto.ModuleStatsDto;
import com.prototype.proxy.logging.ProxyExecutionLog;
import com.prototype.proxy.logging.ProxyExecutionLogRepository;
import com.prototype.proxy.model.SimpleProxyResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StatisticsService {

    private final ProxyExecutionLogRepository logRepository;

    public SimpleProxyResponse getDailyModuleStats(LocalDate date) {
        return executeStatsAction(() -> {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            List<ModuleStatsDto> stats = logRepository.getModuleStatistics(start, end);
            return Map.of("stats", stats);
        });
    }

    public SimpleProxyResponse getModuleDetailStats(LocalDate date, String module) {
        return executeStatsAction(() -> {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            List<InterfaceStatsDto> stats = logRepository.getInterfaceStatistics(start, end, module);
            return Map.of("stats", stats);
        });
    }

    public SimpleProxyResponse getErrorLogs(LocalDate date, String interfaceId) {
        return executeStatsAction(() -> {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            List<ProxyExecutionLog> logs = logRepository.findErrorLogs(start, end, interfaceId);
            return Map.of("logs", logs);
        });
    }

    public SimpleProxyResponse executeStatsAction(Supplier<Map<String, Object>> action) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> data = action.get();
            long executionTime = System.currentTimeMillis() - startTime;

            return SimpleProxyResponse.success(data, requestId, executionTime);
        } catch (Exception e) {
            log.error("Statistics Error (RequestId: {})", requestId, e);
            return SimpleProxyResponse.error(e.getMessage(), requestId);
        }
    }
}
