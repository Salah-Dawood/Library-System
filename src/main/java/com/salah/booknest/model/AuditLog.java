package com.salah.booknest.model;


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

    private String type;
    private Long userId;//instigator
    private String action;//what happened ->
    private Long whatId;// -> to what/who
    private LocalDateTime timestamp; // when it happened

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}
