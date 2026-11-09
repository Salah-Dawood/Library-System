package com.salah.booknest.notification.config;

import com.salah.booknest.notification.event.EventListener;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * The topics this service reads. They are created on startup if they do not exist yet, so this service
 * can start before the services that write to them.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(EventListener.USER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(EventListener.LOAN_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic waitlistEventsTopic() {
        return TopicBuilder.name(EventListener.WAITLIST_EVENTS).partitions(3).replicas(1).build();
    }
}
