package com.acorn.abc.statistic.repository;

import com.acorn.abc.statistic.domain.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query(value = "SELECT a FROM AdminAuditLog a LEFT JOIN FETCH a.adminMember " +
            "WHERE (:actionType IS NULL OR a.actionType = :actionType) " +
            "AND (:targetType IS NULL OR a.targetType = :targetType) " +
            "AND (:targetId IS NULL OR a.targetId = :targetId) " +
            "ORDER BY a.createdAt DESC",
            countQuery = "SELECT COUNT(a) FROM AdminAuditLog a " +
            "WHERE (:actionType IS NULL OR a.actionType = :actionType) " +
            "AND (:targetType IS NULL OR a.targetType = :targetType) " +
            "AND (:targetId IS NULL OR a.targetId = :targetId)")
    Page<AdminAuditLog> search(@Param("actionType") String actionType,
                                @Param("targetType") String targetType,
                                @Param("targetId") Long targetId,
                                Pageable pageable);
}
