package com.prototype.proxy.registry;

import lombok.Data;

import java.util.List;

/**
 * 인터페이스 정의 (YAML 파일과 매핑됨)
 */
@Data
public class InterfaceDefinition {

    private String id;
    private String name;
    private String description;
    private String rfcFunction;

    private List<ImportMapping> importMapping;
    private List<TableMapping> tableMapping;
    private List<ExportMapping> exportMapping;
    private List<ReturnTableMapping> returnTableMapping;

    @Data
    public static class FieldMapping {

        private String webField;
        private String sapField;
        private String type;
        private boolean required;
        private int size;
        private String defaultValue;
    }

    @Data
    public static class ImportMapping {

        private String webField;
        private String sapField;
        private String type;
        private boolean required;
        private int size;
        private String defaultValue;
    }

    @Data
    public static class TableMapping {

        private String webFields;
        private String sapTable;
        private boolean singleValue;
        private boolean required;
        private List<FieldMapping> fields;
    }

    @Data
    public static class ExportMapping {

        private String sapParam;
        private String webField;
        private String type;
        private int size;
    }

    @Data
    public static class ReturnTableMapping {

        private String sapReturnTable;
        private String webReturnList;
        private List<FieldMapping> fields;
    }
}
