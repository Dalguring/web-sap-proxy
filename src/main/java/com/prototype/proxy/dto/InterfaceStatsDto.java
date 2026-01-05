package com.prototype.proxy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceStatsDto {
    private String interfaceId;
    private String rfcFunction;
    private long totalCount;
    private long successCount;
    private long failCount;
}