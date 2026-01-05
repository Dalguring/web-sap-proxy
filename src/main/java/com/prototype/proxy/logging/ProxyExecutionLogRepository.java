package com.prototype.proxy.logging;

import com.prototype.proxy.dto.InterfaceStatsDto;
import com.prototype.proxy.dto.ModuleStatsDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Proxy Log Repository
 */
@Repository
public interface ProxyExecutionLogRepository extends JpaRepository<ProxyExecutionLog, Long> {

    /**
     * Request ID로 조회
     */
    ProxyExecutionLog findByRequestId(String requestId);

    @Query("SELECT new com.prototype.proxy.dto.ModuleStatsDto (" +
           "  COALESCE(l.sapModule, 'UNKNOWN'), " +
           "  COUNT(l)," +
           "  SUM(CASE WHEN l.success = true THEN 1 ELSE 0 END), " +
           "  SUM(CASE WHEN l.success = false THEN 1 ELSE 0 END) " +
           ")" +
           "FROM ProxyExecutionLog l " +
           "WHERE l.createdAt BETWEEN :start AND :end " +
           "GROUP BY COALESCE(l.sapModule, 'UNKNOWN') ")
    List<ModuleStatsDto> getModuleStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new com.prototype.proxy.dto.InterfaceStatsDto(" +
           "  l.interfaceId, " +
           "  MAX(l.rfcFunction), " +
           "  COUNT(l), " +
           "  SUM(CASE WHEN l.success = true THEN 1 ELSE 0 END), " +
           "  SUM(CASE WHEN l.success = false THEN 1 ELSE 0 END) " +
           ") " +
           "FROM ProxyExecutionLog l " +
           "WHERE l.createdAt BETWEEN :start AND :end " +
           "AND (COALESCE(l.sapModule, 'UNKNOWN') = :module OR :module = 'ALL') " +
           "GROUP BY l.interfaceId")
    List<InterfaceStatsDto> getInterfaceStatistics(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("module") String module
    );

    @Query("SELECT l FROM ProxyExecutionLog l " +
           "WHERE l.createdAt BETWEEN :start AND :end " +
           "AND l.interfaceId = :interfaceId " +
           "AND l.success = false")
    List<ProxyExecutionLog> findErrorLogs(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("interfaceId") String interfaceId);
}
