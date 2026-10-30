package com.aoo.bcg.activity;
import java.time.*;import java.util.*;
public interface ActivityRepository {
 List<ActivityModels.Activity> active(Instant now); Optional<ActivityModels.Activity> find(String code,long version);
 ActivityModels.CheckIn checkIn(long player,ActivityModels.Activity activity,LocalDate day,ActivityModels.Reward reward);
 ActivityModels.Progress addProgress(long player,ActivityModels.Activity activity,String mission,String eventId,long delta);
 ActivityModels.Claim reserveMissionClaim(long player,ActivityModels.Activity activity,String mission);
 void completeClaim(String key); void failClaim(String key,String reason);
}
