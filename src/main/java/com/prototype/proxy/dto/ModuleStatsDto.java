package com.prototype.proxy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModuleStatsDto {
    private String sapModule;
    private long totalCount;
    private long successCount;
    private long failCount;
}