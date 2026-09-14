package com.aoo.bcg.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.time.Duration;
import java.util.function.Consumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;

/** Node-scoped consumer used by hall/game/config nodes; failures are retried by RocketMQ. */
public final class RocketMqOutboxConsumer implements AutoCloseable {
    private final DefaultMQPushConsumer consumer;
    private final EventSchemaRegistry schemas=new EventSchemaRegistry();

    public RocketMqOutboxConsumer(String nameserver, String consumerGroup, String topic,
            ObjectMapper mapper, JdbcConsumedEventStore consumedEvents,
            Consumer<OutboxEvent> handler) throws Exception {
        if (nameserver == null || nameserver.isBlank() || consumerGroup == null || consumerGroup.isBlank())
            throw new IllegalArgumentException("RocketMQ nameserver and consumer group are required");
        Objects.requireNonNull(mapper, "mapper");
        Objects.requireNonNull(consumedEvents, "consumedEvents");
        Objects.requireNonNull(handler, "handler");
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameserver);
        // Every node owns a unique group, so clustering gives every node a copy while
        // retaining broker-managed offsets and reliable retry semantics.
        consumer.setMessageModel(MessageModel.CLUSTERING);
        // A node starting after publication must still refresh to the active profile.
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.setMaxReconsumeTimes(5);
        consumer.subscribe(topic == null || topic.isBlank() ? RocketMqOutboxPublisher.DEFAULT_TOPIC : topic,
                "GAME_PROFILE_ACTIVATED");
        consumer.registerMessageListener((org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly)
                (messages, context) -> {
                    try {
                        for (var message : messages) {
                            OutboxEvent event = mapper.readValue(message.getBody(), OutboxEvent.class);
                            schemas.requireSupported(event.eventType(),event.schemaVersion());
                            if (!"GAME_PROFILE_ACTIVATED".equals(event.eventType())) continue;
                            var claim=consumedEvents.claimLease(event.eventId(), Duration.ofMinutes(5));
                            if (claim.isEmpty()) continue;
                            try {
                                handler.accept(event);
                                consumedEvents.complete(claim.orElseThrow(),ConsumedEventResult.success());
                            } catch (Exception error) {
                                boolean dead=consumedEvents.recordFailure(claim.orElseThrow(),event,error,5);
                                if(dead){System.err.println("AOO_DLQ_ALERT consumer="+consumerGroup+" eventId="+event.eventId());continue;}
                                throw error;
                            }
                        }
                        return ConsumeOrderlyStatus.SUCCESS;
                    } catch (Exception error) {
                        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                    }
                });
        consumer.start();
    }

    @Override public void close() { consumer.shutdown(); }
}
