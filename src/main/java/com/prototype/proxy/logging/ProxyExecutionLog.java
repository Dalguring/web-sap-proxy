package com.prototype.proxy.logging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proxy_execution_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String requestId;

    @Column(nullable = false, length = 100)
    private String interfaceId;

    @Column(length = 20)
    private String sapModule;

    @Column(length = 100)
    private String rfcFunction;

    @Column(length = 50)
    private String userId;

    @Column(length = 50)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String requestData;

    @Column(columnDefinition = "TEXT")
    private String responseData;

    @Column
    private Boolean success;

    @Column(length = 1000)
    private String errorMessage;

    @Column
    private Long executionTimeMs;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
