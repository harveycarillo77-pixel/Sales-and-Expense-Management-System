package com.example.sales_expense_system.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.sales_expense_system.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<AuditLog> findAllByOrderByActionTimestampDesc();

    @EntityGraph(attributePaths = {"user"})
    @Query("""
        SELECT a
        FROM AuditLog a
        WHERE (:userId IS NULL OR a.user.userId = :userId)
          AND (:username IS NULL OR LOWER(a.user.username) LIKE LOWER(CONCAT('%', :username, '%')))
          AND (:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%')))
          AND (:tableName IS NULL OR LOWER(a.tableName) LIKE LOWER(CONCAT('%', :tableName, '%')))
          AND (:moduleName IS NULL OR LOWER(a.moduleName) LIKE LOWER(CONCAT('%', :moduleName, '%')))
          AND (:targetDisplay IS NULL OR LOWER(a.targetDisplay) LIKE LOWER(CONCAT('%', :targetDisplay, '%')))
          AND (:summary IS NULL OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :summary, '%')))
          AND (:dateFrom IS NULL OR a.actionTimestamp >= :dateFrom)
          AND (:dateTo IS NULL OR a.actionTimestamp <= :dateTo)
        ORDER BY a.actionTimestamp DESC
    """)
    List<AuditLog> searchLogs(
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("action") String action,
            @Param("tableName") String tableName,
            @Param("moduleName") String moduleName,
            @Param("targetDisplay") String targetDisplay,
            @Param("summary") String summary,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );
}