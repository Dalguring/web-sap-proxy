package com.prototype.proxy.service;

import com.prototype.proxy.context.RequestContext;
import com.prototype.proxy.exception.InterfaceMappingException;
import com.prototype.proxy.registry.InterfaceDefinition.FieldMapping;
import com.prototype.proxy.registry.InterfaceDefinition.ReturnTableMapping;
import com.prototype.proxy.registry.InterfaceDefinition.ExportMapping;
import com.prototype.proxy.registry.InterfaceDefinition.ImportMapping;
import com.prototype.proxy.registry.InterfaceDefinition.TableMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * WEB ↔ SAP 데이터 매핑 엔진
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MappingEngine {

    private final RequestContext requestContext;

    /**
     * 공통 값 검증 메서드
     */
    private Object validate(String field, Object value, boolean required, int size, String defaultValue) {
        String interfaceId = requestContext.getInterfaceId();

        if (size <= 0) {
            throw new InterfaceMappingException(interfaceId, "Size configuration missing for: " + field);
        }

        if (ObjectUtils.isEmpty(value) && defaultValue != null) {
            value = defaultValue;
        }

        if (ObjectUtils.isEmpty(value)) {
            if (required) {
                throw new InterfaceMappingException(interfaceId, "Required field missing: " + field);
            }
            return null;
        }

        if (String.valueOf(value).length() > size) {
            throw new InterfaceMappingException(interfaceId,
                    String.format("Size exceeded, field: %s (Max: %d, Actual: %d)",
                            field, size, String.valueOf(value).length()));
        }

        return value;
    }

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
            Object value = validate(
                    mapping.getWebField(),
                    webData.get(mapping.getWebField()),
                    mapping.isRequired(),
                    mapping.getSize(),
                    mapping.getDefaultValue()
            );

            if (value != null) {
                importParams.put(mapping.getSapField(), value);
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

        for (TableMapping tableMapping : mappings) {
            Object webValue = webData.get(tableMapping.getWebFields());

            // 테이블 필수 데이터가 비어있는지 체크
            if (ObjectUtils.isEmpty(webValue)) {
                if (tableMapping.isRequired()) {
                    throw new InterfaceMappingException(requestContext.getInterfaceId(),
                            "Required table missing: " + tableMapping.getWebFields());
                }
                continue;
            }

            // 필드 매핑 설정 정보가 없는 경우
            if (ObjectUtils.isEmpty(tableMapping.getFields())) {
                throw new InterfaceMappingException(
                        requestContext.getInterfaceId(),
                        "No fields configured for table mapping: SAP table [" + tableMapping.getSapTable() + "]"
                );
            }

            List<Map<String, Object>> tableRows = new ArrayList<>();

            if (tableMapping.isSingleValue()) {
                Map<String, Object> row = new HashMap<>();

                for (FieldMapping fieldMapping : tableMapping.getFields()) {
                    Object rawValue;

                    if (webValue instanceof Map) {
                        rawValue = ((Map<String, Object>) webValue).get(fieldMapping.getWebField());
                    } else {
                        rawValue = webValue;
                    }

                    Object validatedValue = validate(
                            tableMapping.getWebFields() + "." + fieldMapping.getWebField(),
                            rawValue,
                            fieldMapping.isRequired(),
                            fieldMapping.getSize(),
                            fieldMapping.getDefaultValue()
                    );

                    if (validatedValue != null) {
                        row.put(fieldMapping.getSapField(), validatedValue);
                    }
                }

                tableRows.add(row);
                log.trace("Mapped single value to table: {} -> {} = {}",
                        tableMapping.getWebFields(), tableMapping.getSapTable(), webValue);
            }
            else {
                if (!(webValue instanceof List)) {
                    throw new InterfaceMappingException(requestContext.getInterfaceId(),
                            "Field must be array/list: " + tableMapping.getWebFields());
                }

                List<Map<String, Object>> rows = (List<Map<String, Object>>) webValue;

                for (int i = 0; i < rows.size(); i++) {
                    Map<String, Object> webRow = rows.get(i);
                    Map<String, Object> sapRow = new HashMap<>();

                    for (FieldMapping fieldMapping : tableMapping.getFields()) {
                        Object rawValue = webRow.get(fieldMapping.getWebField());
                        Object validatedValue = validate(
                                tableMapping.getWebFields() + "[" + i + "]." + fieldMapping.getWebField(),
                                rawValue,
                                fieldMapping.isRequired(),
                                fieldMapping.getSize(),
                                fieldMapping.getDefaultValue()
                        );

                        if (validatedValue != null) {
                            sapRow.put(fieldMapping.getSapField(), validatedValue);
                        }
                    }
                    tableRows.add(sapRow);
                }
                log.trace("Mapped table: {} -> {} ({} rows)", tableMapping.getWebFields(), tableMapping.getSapTable(), rows.size());
            }

            tables.put(tableMapping.getSapTable(), tableRows);
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
            if (!ObjectUtils.isEmpty(value)) {
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
            List<Map<String, Object>> sapTable = returnTables.get(mapping.getSapTable());

            if (ObjectUtils.isEmpty(sapTable)) {
                continue;
            }

            List<Map<String, Object>> webRows = new ArrayList<>();

            for (Map<String, Object> sapRow : sapTable) {
                Map<String, Object> webRow = new HashMap<>();

                for (FieldMapping fieldMapping : mapping.getFields()) {
                    Object value = sapRow.get(fieldMapping.getSapField());
                    if (!ObjectUtils.isEmpty(value)) {
                        webRow.put(fieldMapping.getWebField(), value);
                    }
                }

                webRows.add(webRow);
            }

            result.put(mapping.getWebField(), webRows);
        }

        return result;
    }
}
