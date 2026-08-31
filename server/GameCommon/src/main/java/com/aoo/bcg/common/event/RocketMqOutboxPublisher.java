package com.aoo.bcg.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.client.producer.MessageQueueSelector;

/** Synchronous publisher: an outbox row is acknowledged only after the broker confirms SEND_OK. */
public final class RocketMqOutboxPublisher implements OutboxPublisher, AutoCloseable {
    public static final String DEFAULT_TOPIC = "AOO_GAME_PROFILE_EVENTS";
    private final DefaultMQProducer producer;
    private final ObjectMapper mapper;
    private final String topic;

    public RocketMqOutboxPublisher(String nameserver, String producerGroup, String topic,
            ObjectMapper mapper) throws Exception {
        if (nameserver == null || nameserver.isBlank() || producerGroup == null || producerGroup.isBlank())
            throw new IllegalArgumentException("RocketMQ nameserver and producer group are required");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.topic = topic == null || topic.isBlank() ? DEFAULT_TOPIC : topic;
        this.producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameserver);
        producer.setRetryTimesWhenSendFailed(2);
        producer.start();
    }

    @Override public void publish(OutboxEvent event) throws Exception {
        byte[] body = mapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
        Message message = new Message(topic, event.eventType(), event.eventId(), body);
        String partitionKey=event.aggregateType()+":"+event.aggregateId();
        MessageQueueSelector selector=(queues,msg,key)->queues.get(Math.floorMod(key.hashCode(),queues.size()));
        var result = producer.send(message,selector,partitionKey,5_000);
        if (result == null || result.getSendStatus() != SendStatus.SEND_OK)
            throw new IllegalStateException("RocketMQ did not confirm SEND_OK");
    }

    @Override public void close() { producer.shutdown(); }
}
