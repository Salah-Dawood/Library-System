package com.salah.booknest.lending.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "waitlist_entries")
@ToString
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // users and books belong to other services, so only their ids and a copy of the name and title are kept
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column
    private String username;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column
    private String bookTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @CreationTimestamp
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;
}
