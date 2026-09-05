package com.aoo.bcg.gateway;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
final class WaitingRoomExpirationPolicyTest{
 private final Instant created=Instant.parse("2026-08-24T00:00:00Z");
 @Test void expiresExactlyAtThreeHundredSecondsOnlyBeforeFirstRound(){assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(299),Map.of("phase","WAITING","roundNo",0)));assertTrue(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(300),Map.of("phase","WAITING","roundNo",0)));assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(300),Map.of("phase","PLAYING","roundNo",1)));}
 @Test void settledOrPreviouslyStartedRoomsNeverExpire(){assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(600),Map.of("phase","FINISHED","roundNo",1,"started",true)));assertFalse(WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(600),Map.of("phase","WAITING","roundNo",2)));}
 @Test void unknownSnapshotsFailClosed(){assertThrows(IllegalStateException.class,()->WaitingRoomExpirationPolicy.expired(created,created.plusSeconds(300),Map.of("players",Map.of())));}
}
