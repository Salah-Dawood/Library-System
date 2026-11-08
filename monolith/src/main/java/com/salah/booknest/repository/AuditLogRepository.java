package com.salah.booknest.repository;

import com.salah.booknest.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // No "order by" here: the sorting comes from the Pageable
    @Query(value = "select a from AuditLog a where (:type is null or a.type = :type) "
            + "and (:userId is null or a.userId = :userId)",
            countQuery = "select count(a) from AuditLog a where (:type is null or a.type = :type) "
                    + "and (:userId is null or a.userId = :userId)")
    Page<AuditLog> search(@Param("type") String type, @Param("userId") Long userId, Pageable pageable);
}
