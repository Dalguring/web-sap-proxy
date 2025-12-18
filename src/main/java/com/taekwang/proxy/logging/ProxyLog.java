package com.taekwang.proxy.logging;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "proxy_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String requestId;

    @Column(nullable = false, length = 100)
    private String interfaceId;

    @Column(length = 100)
    private String rfcFunction;

    @Column(length = 50)
    private String userId;

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
