package com.aoo.bcg.activity;

import java.time.*;
import java.util.List;

public final class ActivityModels {
 private ActivityModels(){}
 public record Reward(String currency,long amount){public Reward{if(currency==null||!currency.matches("[A-Z0-9_]{1,32}")||amount<=0)throw new IllegalArgumentException("invalid reward");}}
 public record Activity(String code,long version,String title,Instant startsAt,Instant endsAt,ZoneId zone,List<Mission> missions){public Activity{missions=List.copyOf(missions);}}
 public record Mission(String code,String eventType,long target,Reward reward){public Mission{if(target<=0)throw new IllegalArgumentException("target must be positive");}}
 public record Progress(String activityCode,long activityVersion,String missionCode,long value,long target,boolean claimable,boolean claimed){}
 public record CheckIn(LocalDate businessDate,int streak,Reward reward,String claimState){}
 public record Claim(String claimKey,String state,Reward reward){}
}
