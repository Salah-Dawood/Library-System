package com.salah.booknest.user.event;

import com.salah.booknest.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    public static final String TOPIC = "user-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Registration must not fail because Kafka is down, so a failed send is only logged.
     */
    public void userRegistered(User user) {
        UserRegisteredEvent event = UserRegisteredEvent.of(user.getId(), user.getUsername(), user.getEmailAddress());
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(user.getId()), event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("Could not publish UserRegistered for user {}: {}", user.getId(), error.getMessage());
                        } else {
                            log.info("Published UserRegistered for user {}", user.getId());
                        }
                    });
        } catch (RuntimeException e) {
            log.error("Could not publish UserRegistered for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
