package com.taekwang.proxy.service;

import com.sap.conn.jco.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RfcExecutor {
    private final JCoDestination destination;

    /**
     * RFC Function 실행
     *
     * @param functionName RFC Function 이름
     * @param importParams Import 파라미터
     * @param tables Table 파라미터
     * @return Export 파라미터와 Return Table 데이터
     */
    public Map<String, Object> execute(String functionName
            , Map<String, Object> importParams
            , Map<String, List<Map<String, Object>>> tables) throws JCoException {
        log.debug("Executing RFC: {}", functionName);

        JCoFunction function = destination.getRepository().getFunction(functionName);
        if (function == null) {
            throw new IllegalArgumentException("RFC function not found: " + functionName);
        }

        if (importParams != null && !importParams.isEmpty()) {
            setImportParameters(function, importParams);
        }

        if (tables != null && !tables.isEmpty()) {
            setTableParameters(function, tables);
        }

        long startTime = System.currentTimeMillis();
        function.execute(destination);
        long executionTime = System.currentTimeMillis() - startTime;

        log.debug("RFC execution completed in {}ms", executionTime);

        Map<String, Object> result = new HashMap<>();
        result.put("exportParams", extractExportParameters(function));
        result.put("returnTables", extractTableParameters(function));
        result.put("executionTimeMs", executionTime);

        return result;
    }

    private void setImportParameters(JCoFunction function, Map<String, Object> params) {
        JCoParameterList importList = function.getImportParameterList();

        if (importList == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();

            try {
                if (value == null) {
                    continue;
                }

                if (value instanceof String) {
                    importList.setValue(paramName, (String) value);
                } else if (value instanceof Integer) {
                    importList.setValue(paramName, (Integer) value);
                } else if (value instanceof Double) {
                    importList.setValue(paramName, (Double) value);
                } else if (value instanceof Boolean) {
                    importList.setValue(paramName, (Boolean) value);
                } else {
                    importList.setValue(paramName, value.toString());
                }

                log.trace("Set import parameter: {} = {}", paramName, value);

            } catch (Exception e) {
                log.warn("Failed to set import parameter: {}", paramName, e);
            }
        }
    }

    private void setTableParameters(JCoFunction function, Map<String, List<Map<String, Object>>> tables) {
        JCoParameterList tableList = function.getTableParameterList();

        if (tableList == null) {
            return;
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            List<Map<String, Object>> rows = entry.getValue();

            try {
                JCoTable table = tableList.getTable(tableName);

                for (Map<String, Object> row : rows) {
                    table.appendRow();

                    for (Map.Entry<String, Object> field : row.entrySet()) {
                        String fieldName = field.getKey();
                        Object value = field.getValue();

                        if (value != null) {
                            table.setValue(fieldName, value);
                        }
                    }
                }

                log.trace("Set table: {} with {} rows", tableName, rows.size());

            } catch (Exception e) {
                log.warn("Failed to set table: {}", tableName, e);
            }
        }
    }

    private Map<String, Object> extractExportParameters(JCoFunction function) {
        Map<String, Object> exports = new HashMap<>();
        JCoParameterList exportList = function.getExportParameterList();

        if (exportList == null) {
            return exports;
        }

        for (JCoField field : exportList) {
            try {
                exports.put(field.getName(), field.getValue());
            } catch (Exception e) {
                log.warn("Failed to extract export parameter: {}", field.getName(), e);
            }
        }

        return exports;
    }

    private Map<String, List<Map<String, Object>>> extractTableParameters(JCoFunction function) {
        Map<String, List<Map<String, Object>>> tables = new HashMap<>();
        JCoParameterList tableList = function.getTableParameterList();

        if (tableList == null) {
            return tables;
        }

        JCoFieldIterator iterator = tableList.getFieldIterator();

        while (iterator.hasNextField()) {
            JCoField field = iterator.nextField();

            try {
                if (field.isTable()) {
                    String tableName = field.getName();
                    JCoTable table = field.getTable();

                    List<Map<String, Object>> rows = new ArrayList<>();

                    for (int j = 0; j < table.getNumRows(); j++) {
                        table.setRow(j);
                        Map<String, Object> row = new HashMap<>();

                        for (JCoField rowField : table) {
                            row.put(rowField.getName(), rowField.getValue());
                        }

                        rows.add(row);
                    }

                    tables.put(tableName, rows);
                }

            } catch (Exception e) {
                log.warn("Failed to extract table: {}", field.getName(), e);
            }
        }

        return tables;
    }
}
