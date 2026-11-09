package com.salah.booknest.lending.config;

import com.salah.booknest.lending.event.LendingEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // created on startup if they do not exist yet; 3 partitions so several consumers can share the work
    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(LendingEventPublisher.LOAN_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic waitlistEventsTopic() {
        return TopicBuilder.name(LendingEventPublisher.WAITLIST_EVENTS).partitions(3).replicas(1).build();
    }
}
