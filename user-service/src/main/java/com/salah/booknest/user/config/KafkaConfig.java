package com.salah.booknest.user.config;

import com.salah.booknest.user.event.UserEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // created on startup if it does not exist yet; 3 partitions so several consumers can share the work
    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(UserEventPublisher.TOPIC).partitions(3).replicas(1).build();
    }
}
