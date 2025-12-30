package com.prototype.proxy.logging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemAccessLogRepository extends JpaRepository<SystemAccessLog, Long> {
    SystemAccessLog findByRequestId(String requestId);
}
