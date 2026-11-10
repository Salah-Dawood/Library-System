package com.salah.booknest.notification.config;

import com.salah.booknest.notification.event.EventListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

/**
 * The topics this service reads, their dead-letter topics, and what happens when an event fails.
 * The topics are created on startup if they do not exist yet, so this service can start first.
 */
@Slf4j
@Configuration
public class KafkaConfig {

    public static final String DLT_SUFFIX = ".DLT";
    private static final int PARTITIONS = 3;

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(EventListener.USER_EVENTS).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(EventListener.LOAN_EVENTS).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic waitlistEventsTopic() {
        return TopicBuilder.name(EventListener.WAITLIST_EVENTS).partitions(PARTITIONS).replicas(1).build();
    }

    // events that kept failing are parked here, one dead-letter topic per topic
    @Bean
    public NewTopic userEventsDeadLetterTopic() {
        return TopicBuilder.name(EventListener.USER_EVENTS + DLT_SUFFIX).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic loanEventsDeadLetterTopic() {
        return TopicBuilder.name(EventListener.LOAN_EVENTS + DLT_SUFFIX).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic waitlistEventsDeadLetterTopic() {
        return TopicBuilder.name(EventListener.WAITLIST_EVENTS + DLT_SUFFIX).partitions(PARTITIONS).replicas(1).build();
    }

    /**
     * A failed event is tried again 3 times, 2 seconds apart (the mail server may be back by then).
     * If it still fails it is moved to its dead-letter topic, so the events behind it are not blocked.
     * An event that is not valid JSON is moved there straight away: trying again cannot fix it.
     * Spring Boot uses this bean for every @KafkaListener.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (record, error) -> {
            log.error("Event from {} (partition {}, offset {}) failed and was moved to {}: {}",
                    record.topic(), record.partition(), record.offset(), record.topic() + DLT_SUFFIX, error.getMessage());
            return new TopicPartition(record.topic() + DLT_SUFFIX, record.partition());
        });
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));
        errorHandler.addNotRetryableExceptions(JacksonException.class);
        return errorHandler;
    }
}
