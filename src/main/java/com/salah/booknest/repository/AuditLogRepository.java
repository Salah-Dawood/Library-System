package com.salah.booknest.repository;

import com.salah.booknest.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("select a from AuditLog a where (:type is null or a.type = :type) "
            + "and (:userId is null or a.userId = :userId) order by a.timestamp desc")
    List<AuditLog> search(@Param("type") String type, @Param("userId") Long userId);
}
