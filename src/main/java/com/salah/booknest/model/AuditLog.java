package com.salah.booknest.model;


import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Long userId;//instigator

    @Column(nullable = false)
    private String action;//what happened ->

    @Column(nullable = false)
    private Long whatId;// -> to what/who

    @Column(nullable = false)
    private LocalDateTime timestamp; // when it happened

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}
