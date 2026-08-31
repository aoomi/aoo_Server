package com.aoo.bcg.activity;
import java.time.*;import java.util.*;

public final class ActivityService {
 public interface RewardPort{void grant(String businessId,long player,String currency,long amount,String reason);}
 private final ActivityRepository repository;private final RewardPort rewards;private final Clock clock;
 public ActivityService(ActivityRepository repository,RewardPort rewards,Clock clock){this.repository=Objects.requireNonNull(repository);this.rewards=Objects.requireNonNull(rewards);this.clock=Objects.requireNonNull(clock);}
 public List<ActivityModels.Activity> catalog(){return repository.active(clock.instant());}
 public ActivityModels.CheckIn checkIn(long player,String activityCode){var a=current(activityCode);LocalDate day=clock.instant().atZone(a.zone()).toLocalDate();var mission=a.missions().stream().filter(m->"DAILY_CHECK_IN".equals(m.eventType())).findFirst().orElseThrow(()->new IllegalStateException("check-in is not configured"));var result=repository.checkIn(player,a,day,mission.reward());grant(result.claimState(),"checkin:"+a.code()+":"+a.version()+":"+player+":"+day,player,result.reward(),"ACTIVITY_CHECK_IN");return "RESERVED".equals(result.claimState())?new ActivityModels.CheckIn(result.businessDate(),result.streak(),result.reward(),"COMPLETED"):result;}
 public ActivityModels.Progress progress(long player,String activityCode,String mission,String eventId,long delta){if(eventId==null||!eventId.matches("[A-Za-z0-9_.:-]{1,128}")||delta<=0)throw new IllegalArgumentException("invalid progress event");return repository.addProgress(player,current(activityCode),mission,eventId,delta);}
 public ActivityModels.Claim claim(long player,String activityCode,String mission){var a=current(activityCode);var c=repository.reserveMissionClaim(player,a,mission);grant(c.state(),c.claimKey(),player,c.reward(),"MISSION_REWARD");return "COMPLETED".equals(c.state())?c:new ActivityModels.Claim(c.claimKey(),"COMPLETED",c.reward());}
 private void grant(String state,String key,long player,ActivityModels.Reward reward,String reason){if("COMPLETED".equals(state))return;try{rewards.grant(key,player,reward.currency(),reward.amount(),reason);repository.completeClaim(key);}catch(RuntimeException e){repository.failClaim(key,e.getClass().getSimpleName());throw e;}}
 private ActivityModels.Activity current(String code){return catalog().stream().filter(a->a.code().equals(code)).findFirst().orElseThrow(()->new NoSuchElementException("activity is not active"));}
}
