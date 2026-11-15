package com.salah.booknest.lending.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The shedlock table: one row per scheduled job, saying which instance holds it and until when.
 * ShedLock reads and writes it with plain SQL; this class only lets Hibernate create the table.
 */
@Entity
@Table(name = "shedlock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchedulerLock {

    @Id
    @Column(length = 64)
    private String name;

    @Column(name = "lock_until", nullable = false, columnDefinition = "timestamp")
    private LocalDateTime lockUntil;

    @Column(name = "locked_at", nullable = false, columnDefinition = "timestamp")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", nullable = false)
    private String lockedBy;
}
