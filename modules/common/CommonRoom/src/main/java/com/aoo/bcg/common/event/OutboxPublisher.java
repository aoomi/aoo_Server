package com.aoo.bcg.common.event;

@FunctionalInterface
public interface OutboxPublisher {
    void publish(OutboxEvent event) throws Exception;
}
