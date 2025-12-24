package com.prototype.proxy.model;

import lombok.Data;

/**
 * WEB 필드와 SAP RFC 필드 간의 매핑 정보
 */
@Data
public class FieldMapping {
    private String webField;
    private String sapField;
    private String type;
    private boolean required;
    private int size;
    private String defaultValue;
}
