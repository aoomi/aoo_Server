package com.aoo.bcg.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class RocketMqOutboxIntegrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "AOO_MQ_IT_DB_URL", matches = ".+")
    void staleLeaseIsFencedAndRetentionCleanupIsBounded() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","");var dataSource=new DriverManagerDataSource(System.getenv("AOO_MQ_IT_DB_URL"),System.getenv("AOO_MQ_IT_DB_USER"),System.getenv("AOO_MQ_IT_DB_PASSWORD"));var store=new JdbcConsumedEventStore(dataSource,"lease:"+suffix);var first=store.claimLease("event-"+suffix,Duration.ofSeconds(1)).orElseThrow();try(var c=dataSource.getConnection();var q=c.prepareStatement("UPDATE aoo_consumed_event SET claimed_at=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 2 SECOND) WHERE consumer_name=? AND event_id=?")){q.setString(1,"lease:"+suffix);q.setString(2,"event-"+suffix);q.executeUpdate();}var second=store.claimLease("event-"+suffix,Duration.ofSeconds(1)).orElseThrow();assertThrows(IllegalStateException.class,()->store.complete(first,ConsumedEventResult.success()));store.complete(second,ConsumedEventResult.success());try(var c=dataSource.getConnection();var q=c.prepareStatement("UPDATE aoo_consumed_event SET processed_at=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 2 DAY) WHERE consumer_name=?")){q.setString(1,"lease:"+suffix);q.executeUpdate();}assertEquals(1,store.cleanup(Instant.now().minus(Duration.ofDays(1)),Instant.now().minus(Duration.ofDays(1)),100));
    }
    @Test
    @EnabledIfEnvironmentVariable(named = "AOO_MQ_IT_NAMESRV", matches = ".+")
    void brokerRoundTripAndDatabaseDeduplication() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String nameserver = System.getenv("AOO_MQ_IT_NAMESRV");
        String topic = RocketMqOutboxPublisher.DEFAULT_TOPIC;
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var dataSource = new DriverManagerDataSource(System.getenv("AOO_MQ_IT_DB_URL"),
                System.getenv("AOO_MQ_IT_DB_USER"), System.getenv("AOO_MQ_IT_DB_PASSWORD"));
        var store = new JdbcConsumedEventStore(dataSource, "it:" + suffix);
        var handled = new AtomicInteger();
        var latch = new CountDownLatch(1);
        OutboxEvent event = new OutboxEvent("event-" + suffix, "game-profile", 9,
                "GAME_PROFILE_ACTIVATED", Map.of("version", "v1"), Instant.now());
        try (var publisher = new RocketMqOutboxPublisher(nameserver, "producer-" + suffix, topic, mapper)) {
            publisher.publish(event); // creates the topic before subscription
            try (var consumer = new RocketMqOutboxConsumer(nameserver, "consumer-" + suffix,
                    topic, mapper, store, received -> {
                        if (event.eventId().equals(received.eventId())) {
                            handled.incrementAndGet();
                            latch.countDown();
                        }
                    })) {
                long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
                while (latch.getCount() > 0 && System.nanoTime() < deadline) {
                    publisher.publish(event);
                    latch.await(500, TimeUnit.MILLISECONDS);
                }
                assertTrue(latch.getCount() == 0, "consumer did not become ready before deadline");
                publisher.publish(event);
                Thread.sleep(1_000);
            }
        }
        assertEquals(1, handled.get());
        assertTrue(store.alreadyProcessed(event.eventId()));
    }
}
