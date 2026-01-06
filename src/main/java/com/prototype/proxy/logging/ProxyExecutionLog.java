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
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "interface_id", nullable = false, length = 100)
    private String interfaceId;

    @Column(name = "sap_module", length = 20)
    private String sapModule;

    @Column(name = "rfc_function", length = 100)
    private String rfcFunction;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "execution_time")
    private Long executionTimeMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
