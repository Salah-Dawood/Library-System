package com.salah.booknest.lending.cucumber;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * Reads what lending-service published to the embedded Kafka broker, from the start of a topic.
 */
@Component
public class KafkaTopics {

    private final EmbeddedKafkaBroker broker;

    public KafkaTopics(EmbeddedKafkaBroker broker) {
        this.broker = broker;
    }

    /** Waits up to 20 seconds for a message that matches, then returns every message of the topic read so far. */
    public List<ConsumerRecord<String, String>> waitFor(String topic, Predicate<ConsumerRecord<String, String>> found) {
        try (KafkaConsumer<String, String> consumer = consumer(topic)) {
            List<ConsumerRecord<String, String>> seen = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                    seen.add(record);
                    if (found.test(record)) {
                        return drain(consumer, seen);
                    }
                }
            }
            throw new AssertionError("the expected event never arrived on " + topic + "; it holds " + seen.size() + " events");
        }
    }

    /** Every message the topic holds right now. */
    public List<ConsumerRecord<String, String>> all(String topic) {
        try (KafkaConsumer<String, String> consumer = consumer(topic)) {
            return drain(consumer, new ArrayList<>());
        }
    }

    // reads on until the end of every partition (the end is fixed when this starts)
    private List<ConsumerRecord<String, String>> drain(KafkaConsumer<String, String> consumer,
                                                       List<ConsumerRecord<String, String>> seen) {
        Map<TopicPartition, Long> end = consumer.endOffsets(consumer.assignment());
        long deadline = System.currentTimeMillis() + 20_000;
        while (end.entrySet().stream().anyMatch(e -> consumer.position(e.getKey()) < e.getValue())) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("could not read to the end of the topic");
            }
            consumer.poll(Duration.ofMillis(200)).forEach(seen::add);
        }
        return seen;
    }

    // assigned to every partition directly (no consumer group to join), starting at the beginning
    private KafkaConsumer<String, String> consumer(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                .map(info -> new TopicPartition(topic, info.partition())).toList();
        consumer.assign(partitions);
        consumer.seekToBeginning(partitions);
        return consumer;
    }
}
