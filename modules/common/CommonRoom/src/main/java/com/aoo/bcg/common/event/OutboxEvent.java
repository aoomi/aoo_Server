package com.aoo.bcg.common.event;
import java.time.Instant;
public record OutboxEvent(String eventId, String aggregateType, long aggregateId, String eventType,int schemaVersion,Object payload, Instant createdAt) {
    public OutboxEvent{if(schemaVersion<=0)throw new IllegalArgumentException("event schema version must be positive");}
    public OutboxEvent(String eventId,String aggregateType,long aggregateId,String eventType,Object payload,Instant createdAt){this(eventId,aggregateType,aggregateId,eventType,1,payload,createdAt);}
}
