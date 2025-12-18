package com.taekwang.proxy.service;

import com.taekwang.proxy.model.FieldMapping;
import com.taekwang.proxy.registry.InterfaceDefinition.ReturnTableMapping;
import com.taekwang.proxy.registry.InterfaceDefinition.ExportMapping;
import com.taekwang.proxy.registry.InterfaceDefinition.ImportMapping;
import com.taekwang.proxy.registry.InterfaceDefinition.TableMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WEB ↔ SAP 데이터 매핑 엔진
 * <p>
 * Note: 실제 JCO 연동 전까지는 매핑 로직만 구현
 */
@Slf4j
@Service
public class MappingEngine {
    /**
     * WEB 데이터 → SAP RFC Import 파라미터 매핑
     *
     * @param webData  WEB에서 전송한 데이터
     * @param mappings Import 매핑 정의
     * @return 매핑된 Import 파라미터
     */
    public Map<String, Object> mapImportParameters(Map<String, Object> webData, List<ImportMapping> mappings) {
        Map<String, Object> importParams = new HashMap<>();

        if (mappings == null || mappings.isEmpty()) {
            return importParams;
        }

        for (ImportMapping mapping : mappings) {
            Object value = webData.get(mapping.getWebField());

            if (value == null && mapping.getDefaultValue() != null) {
                value = mapping.getDefaultValue();
            }

            if (value == null && mapping.isRequired()) {
                throw new IllegalArgumentException("Required field missing: " + mapping.getWebField());
            }

            if (value != null) {
                importParams.put(mapping.getSapParam(), value);
                log.trace("Mapped import: {} -> {} = {}", mapping.getWebField(), mapping.getSapParam(), value);
            }
        }

        return importParams;
    }

    /**
     * WEB 데이터 → SAP RFC Table 매핑
     * 단일 값(singleValue=true)과 배열 모두 지원
     *
     * @param webData  WEB에서 전송한 데이터
     * @param mappings Table 매핑 정의
     * @return 매핑된 Table 데이터 (테이블명 → 행 목록)
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<Map<String, Object>>> mapTables(Map<String, Object> webData, List<TableMapping> mappings) {
        Map<String, List<Map<String, Object>>> tables = new HashMap<>();

        if (mappings == null || mappings.isEmpty()) {
            return tables;
        }

        for (TableMapping mapping : mappings) {
            Object webValue = webData.get(mapping.getWebField());

            if (webValue == null) {
                continue;
            }

            List<Map<String, Object>> tableRows = new ArrayList<>();
            if (mapping.isSingleValue()) {
                Map<String, Object> row = new HashMap<>();

                for (FieldMapping fieldMapping : mapping.getFields()) {
                    Object value = null;

                    if (webValue instanceof Map<?, ?>) {
                        value = ((Map<String, Object>) webValue).get(fieldMapping.getWebField());
                    } else {
                        value = webValue;
                    }

                    if (value == null && fieldMapping.getDefaultValue() != null) {
                        value = fieldMapping.getDefaultValue();
                    }

                    if (value != null) {
                        row.put(fieldMapping.getSapField(), value);
                    }
                }

                tableRows.add(row);

                log.trace("Mapped single value to table: {} -> {} = {}",
                        mapping.getWebField(), mapping.getSapTable(), webValue);
            }
            else {
                if (!(webValue instanceof List)) {
                    throw new IllegalArgumentException("Field must be array: " + mapping.getWebField());
                }

                List<Map<String, Object>> rows = (List<Map<String, Object>>) webValue;

                for (Map<String, Object> webRow : rows) {
                    Map<String, Object> sapRow = new HashMap<>();

                    for (FieldMapping fieldMapping : mapping.getFields()) {
                        Object value = webRow.get(fieldMapping.getWebField());

                        // 기본값 처리
                        if (value == null && fieldMapping.getDefaultValue() != null) {
                            value = fieldMapping.getDefaultValue();
                        }

                        if (value != null) {
                            sapRow.put(fieldMapping.getSapField(), value);
                        }
                    }

                    tableRows.add(sapRow);
                }

                log.trace("Mapped table: {} -> {} ({} rows)", mapping.getWebField(), mapping.getSapTable(), rows.size());
            }

            tables.put(mapping.getSapTable(), tableRows);
        }

        return tables;
    }

    /**
     * SAP RFC Export → WEB 응답 매핑
     *
     * @param exportParams SAP Export 파라미터
     * @param mappings     Export 매핑 정의
     * @return 매핑된 응답 데이터
     */
    public Map<String, Object> mapExportParameters(Map<String, Object> exportParams, List<ExportMapping> mappings) {
        Map<String, Object> result = new HashMap<>();

        if (mappings == null || mappings.isEmpty()) {
            return result;
        }

        for (ExportMapping mapping : mappings) {
            Object value = exportParams.get(mapping.getSapParam());
            if (value != null) {
                result.put(mapping.getWebField(), value);

                log.trace("Mapped export: {} -> {} = {}", mapping.getSapParam(), mapping.getWebField(), value);
            }
        }

        return result;
    }

    /**
     * SAP RFC Return Table → WEB 응답 매핑
     *
     * @param returnTables SAP Return Table 데이터
     * @param mappings     Return Table 매핑 정의
     * @return 매핑된 응답 데이터
     */
    public Map<String, Object> mapReturnTables(Map<String, List<Map<String, Object>>> returnTables, List<ReturnTableMapping> mappings) {
        Map<String, Object> result = new HashMap<>();

        if (mappings == null || mappings.isEmpty()) {
            log.warn("No return table mappings defined");
            return result;
        }

        for (ReturnTableMapping mapping : mappings) {
            log.debug("Processing mapping: sapTable={}, webField={}", mapping.getSapTable(), mapping.getWebField());

            List<Map<String, Object>> sapTable = returnTables.get(mapping.getSapTable());

            if (sapTable == null) {
                log.warn("SAP table '{}' not found in returnTables", mapping.getSapTable());
                continue;
            }

            log.debug("Found SAP table '{}' with {} rows", mapping.getSapTable(), sapTable.size());

            List<Map<String, Object>> webRows = new ArrayList<>();

            for (Map<String, Object> sapRow : sapTable) {

                Map<String, Object> webRow = new HashMap<>();

                for (FieldMapping fieldMapping : mapping.getFields()) {
                    Object value = sapRow.get(fieldMapping.getSapField());
                    if (value != null) {
                        webRow.put(fieldMapping.getWebField(), value);
                    }
                }

                webRows.add(webRow);
            }

            result.put(mapping.getWebField(), webRows);

            log.debug("Mapped '{}' -> '{}': {} rows", mapping.getSapTable(), mapping.getWebField(), webRows.size());

            if (!webRows.isEmpty()) {
                log.debug("First mapped row: {}", webRows.get(0));
            }
        }

        return result;
    }
}
