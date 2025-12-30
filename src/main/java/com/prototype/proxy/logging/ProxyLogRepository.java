package com.prototype.proxy.logging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Proxy Log Repository
 */
@Repository
public interface ProxyLogRepository extends JpaRepository<ProxyExecutionLog, Long> {

    /**
     * Request ID로 조회
     */
    ProxyExecutionLog findByRequestId(String requestId);

    /**
     * Interface ID로 조회
     */
    List<ProxyExecutionLog> findByInterfaceIdOrderByCreatedAtDesc(String interfaceId);

    /**
     * 성공/실패 여부로 조회
     */
    List<ProxyExecutionLog> findBySuccessOrderByCreatedAtDesc(Boolean success);

    /**
     * 기간별 조회
     */
    List<ProxyExecutionLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime startDate,
        LocalDateTime endDate
    );
}
