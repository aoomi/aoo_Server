package com.aoo.bcg.gateway;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
final class WaitingRoomExpirationPolicyTest{
 private final Instant created=Instant.parse("2026-08-24T00:00:00Z");
 @Test void expiresExactlyAtTwoHoursOnlyBeforeFirstRound(){assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(7199),Map.of("phase","WAITING","roundNo",0)));assertTrue(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(7200),Map.of("phase","WAITING","roundNo",0)));assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(7200),Map.of("phase","PLAYING","roundNo",1)));}
 @Test void successfulWaitingRoomActivityRestartsTheTwoHourIdleWindow(){Instant joinedAt=created.plusSeconds(7100);assertFalse(WaitingRoomExpirationPolicy.expired(joinedAt,created.plusSeconds(7200),Map.of("phase","WAITING","roundNo",0)));assertTrue(WaitingRoomExpirationPolicy.expired(joinedAt,joinedAt.plusSeconds(7200),Map.of("phase","WAITING","roundNo",0)));}
 @Test void settledOrPreviouslyStartedRoomsNeverExpire(){assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(600),Map.of("phase","FINISHED","roundNo",1,"started",true)));assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(600),Map.of("phase","WAITING","roundNo",2)));}
 @Test void unknownSnapshotsFailClosedWithoutBreakingJoinOrReconnect(){Map<String,Object> unknown=Map.of("players",Map.of());assertFalse(WaitingRoomExpirationPolicy.hasExplicitLifecycleMarker(unknown));assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(7200),unknown));assertTrue(WaitingRoomExpirationPolicy.hasExplicitLifecycleMarker(Map.of("phase","WAITING")));}
 @Test void everyActiveRoomExpiresAfterTwelveHoursWithoutBusinessActivity(){assertFalse(WaitingRoomExpirationPolicy.roomInactive(created.plusSeconds(1),created.plusSeconds(1).plusSeconds(43199)));assertTrue(WaitingRoomExpirationPolicy.roomInactive(created.plusSeconds(1),created.plusSeconds(1).plusSeconds(43200)));assertFalse(WaitingRoomExpirationPolicy.roomInactive(null,created.plusSeconds(50000)));}
}
