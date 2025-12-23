package com.prototype.proxy.service;

import com.prototype.proxy.context.RequestContext;
import com.prototype.proxy.exception.NotFoundException;
import com.prototype.proxy.exception.ProxyException;
import com.prototype.proxy.model.SimpleProxyRequest;
import com.prototype.proxy.registry.InterfaceDefinition;
import com.prototype.proxy.registry.InterfaceRegistry;
import com.prototype.proxy.logging.LoggingService;
import com.prototype.proxy.model.SimpleProxyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {
    private final InterfaceRegistry registry;
    private final MappingEngine mappingEngine;
    private final LoggingService loggingService;
    private final RfcExecutor rfcExecutor;
    private final RequestContext requestContext;

    /**
     * Proxy 요청 실행
     */
    public SimpleProxyResponse execute(SimpleProxyRequest request) {
        long startTime = System.currentTimeMillis();
        // InterfaceId 전역 저장
        requestContext.setInterfaceId(request.getInterfaceId());

        try {
            InterfaceDefinition definition = registry.get(request.getInterfaceId());

            log.info("Executing interface: {} (RFC: {})", definition.getId(), definition.getRfcFunction());

            loggingService.logRequest(request, definition);

            Map<String, Object> importParams = mappingEngine.mapImportParameters(
                    request.getData(),
                    definition.getImportMapping()
            );

            Map<String, List<Map<String, Object>>> tables = mappingEngine.mapTables(
                    request.getData(),
                    definition.getTableMapping()
            );

            log.debug("Mapped import params: {}", importParams);
            log.debug("Mapped tables: {}", tables.keySet());

            // Mock 데이터 생성
//            Map<String, Object> mockSapExport = createMockExportData(definition);
//            Map<String, List<Map<String, Object>>> mockSapTables = createMockTableData(definition);

            Map<String, Object> rfcResult = rfcExecutor.execute(
                    definition.getRfcFunction(),
                    importParams,
                    tables
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> sapExport = (Map<String, Object>) rfcResult.get("exportParams");

            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> sapTables =
                    (Map<String, List<Map<String, Object>>>) rfcResult.get("returnTables");

            Map<String, Object> responseData = new HashMap<>();

            responseData.putAll(mappingEngine.mapExportParameters(
                    sapExport,
                    definition.getExportMapping()
            ));

            responseData.putAll(mappingEngine.mapReturnTables(
                    sapTables,
                    definition.getReturnTableMapping()
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            SimpleProxyResponse response = SimpleProxyResponse.success(
                    responseData,
                    request.getRequestId(),
                    executionTime
            );

            loggingService.logResponse(request, response, definition);
            log.info("Request {} completed in {}ms", request.getRequestId(), executionTime);

            return response;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Request {} failed", request.getRequestId(), e);

            loggingService.logError(request, e);

            throw new ProxyException(e.getMessage(), e, request.getRequestId());
        }
    }

    /**
     * Mock Export 데이터 생성 (임시)
     */
    private Map<String, Object> createMockExportData(InterfaceDefinition definition) {
        Map<String, Object> mockData = new HashMap<>();

        if (definition.getExportMapping() != null) {
            for (InterfaceDefinition.ExportMapping mapping : definition.getExportMapping()) {
                mockData.put(mapping.getSapParam(), "MOCK_" + mapping.getSapParam());
            }
        }

        return mockData;
    }

    /**
     * Mock Table 데이터 생성 (임시)
     */
    private Map<String, List<Map<String, Object>>> createMockTableData(InterfaceDefinition definition) {
        Map<String, List<Map<String, Object>>> mockData = new HashMap<>();

        if (definition.getReturnTableMapping() != null) {
            for (InterfaceDefinition.ReturnTableMapping mapping : definition.getReturnTableMapping()) {
                Map<String, Object> row = new HashMap<>();

                for (var field : mapping.getFields()) {
                    row.put(field.getSapField(), "MOCK_" + field.getSapField());
                }

                mockData.put(mapping.getSapTable(), List.of(row));
            }
        }

        return mockData;
    }

    /**
     * 인터페이스 정의 재로드
     */
    public void reloadInterfaces() {
        registry.reload();
        log.info("Interface definitions reloaded");
    }
}
