package com.salah.booknest.lending.event;

import com.salah.booknest.lending.util.TransactionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class LendingEventPublisher {

    public static final String LOAN_EVENTS = "loan-events";
    public static final String WAITLIST_EVENTS = "waitlist-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Sends the event only once the loan or waitlist change is saved, so nothing is announced for a rollback. */
    public void publishAfterCommit(String topic, LendingEvent event) {
        TransactionUtils.afterCommit(() -> send(topic, event));
    }

    /**
     * The member's id is the key, so all events of one member stay in order on one partition.
     * A failed send is logged and never breaks the loan or waitlist change that caused it.
     */
    public CompletableFuture<?> send(String topic, LendingEvent event) {
        try {
            return kafkaTemplate.send(topic, String.valueOf(event.userId()), event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Could not publish {} event {}: {}", event.type(), event.eventId(), error.getMessage());
                        } else {
                            log.info("Published {} event {} to {}", event.type(), event.eventId(), topic);
                        }
                    });
        } catch (RuntimeException e) {
            log.error("Could not publish {} event {}: {}", event.type(), event.eventId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
