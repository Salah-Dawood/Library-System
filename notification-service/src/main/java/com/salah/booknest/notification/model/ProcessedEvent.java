package com.salah.booknest.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * An event that was handled. Kafka can deliver the same event twice, so its id is checked here
 * first and a repeat is skipped instead of sending a second email.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private String eventId;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}
