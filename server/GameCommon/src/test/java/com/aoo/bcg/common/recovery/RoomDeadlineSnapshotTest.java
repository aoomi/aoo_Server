package com.aoo.bcg.common.recovery;
import java.time.Instant;import java.util.Map;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class RoomDeadlineSnapshotTest{
 @Test void preservesEveryTimerKindAndClassifiesExpiredOnRestart(){var deadlines=new RoomDeadlineSnapshot(Map.of("operation",1000L,"dissolveVote",2000L,"interRound",3000L,"roomExpiration",4000L));var restored=RoomDeadlineSnapshot.fromState(Map.of("deadlines",deadlines.toMap()));assertEquals(RoomDeadlineSnapshot.REQUIRED,restored.deadlineEpochMillis().keySet().stream().sorted(java.util.Comparator.comparingInt(RoomDeadlineSnapshot.REQUIRED::indexOf)).toList());assertEquals(java.util.List.of("operation","dissolveVote"),restored.expiredAt(Instant.ofEpochMilli(2500)));}
}
