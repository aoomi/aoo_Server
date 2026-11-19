package com.aoo.bcg.common.recovery;
import java.time.Instant;import java.util.Map;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class RoomTimerPausePolicyTest{
 @Test void maintenanceFreezesPlayButNeverExtendsRoomExpiration(){var before=new RoomDeadlineSnapshot(Map.of("operation",10_000L,"dissolveVote",20_000L,"interRound",30_000L,"roomExpiration",40_000L));var after=RoomTimerPausePolicy.MAINTENANCE.resume(before,Instant.ofEpochMilli(1000),Instant.ofEpochMilli(6000));assertEquals(15_000L,after.deadlineEpochMillis().get("operation"));assertEquals(25_000L,after.deadlineEpochMillis().get("dissolveVote"));assertEquals(40_000L,after.deadlineEpochMillis().get("roomExpiration"));}
 @Test void tournamentPauseDoesNotFreezeDissolveVote(){var before=new RoomDeadlineSnapshot(Map.of("operation",10_000L,"dissolveVote",20_000L));var after=RoomTimerPausePolicy.TOURNAMENT_PAUSE.resume(before,Instant.EPOCH,Instant.ofEpochSecond(2));assertEquals(12_000L,after.deadlineEpochMillis().get("operation"));assertEquals(20_000L,after.deadlineEpochMillis().get("dissolveVote"));}
}
