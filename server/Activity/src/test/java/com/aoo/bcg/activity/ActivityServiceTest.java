package com.aoo.bcg.activity;
import org.junit.jupiter.api.*;import java.time.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;

class ActivityServiceTest {
 private static final Instant NOW=Instant.parse("2026-08-24T16:30:00Z");
 private final ActivityModels.Reward reward=new ActivityModels.Reward("GOLD",100);
 private final ActivityModels.Activity activity=new ActivityModels.Activity("SUMMER",7,"Summer",NOW.minusSeconds(60),NOW.plusSeconds(3600),ZoneId.of("Asia/Shanghai"),List.of(new ActivityModels.Mission("LOGIN","DAILY_CHECK_IN",1,reward),new ActivityModels.Mission("WIN3","GAME_WIN",3,reward)));
 @Test void timezoneStreakAndDuplicateCheckInUseOneGrant(){FakeRepository repo=new FakeRepository(activity);List<String> grants=new ArrayList<>();var service=new ActivityService(repo,(id,p,c,a,r)->grants.add(id),Clock.fixed(NOW,ZoneOffset.UTC));var first=service.checkIn(9,"SUMMER");var second=service.checkIn(9,"SUMMER");assertEquals(LocalDate.of(2026,8,25),first.businessDate());assertEquals(1,first.streak());assertEquals("COMPLETED",second.claimState());assertEquals(1,grants.size());}
 @Test void progressEventsAndClaimsAreIdempotentAndPinnedToVersion(){FakeRepository repo=new FakeRepository(activity);List<String> grants=new ArrayList<>();var service=new ActivityService(repo,(id,p,c,a,r)->grants.add(id),Clock.fixed(NOW,ZoneOffset.UTC));assertEquals(2,service.progress(9,"SUMMER","WIN3","game:1",2).value());assertEquals(2,service.progress(9,"SUMMER","WIN3","game:1",2).value());assertEquals(3,service.progress(9,"SUMMER","WIN3","game:2",2).value());assertTrue(service.claim(9,"SUMMER","WIN3").claimKey().contains(":7:"));service.claim(9,"SUMMER","WIN3");assertEquals(1,grants.size());}
 @Test void inactiveActivityIsRejected(){var service=new ActivityService(new FakeRepository(activity),(a,b,c,d,e)->{},Clock.fixed(NOW.plusSeconds(7200),ZoneOffset.UTC));assertThrows(NoSuchElementException.class,()->service.checkIn(1,"SUMMER"));}
 private static final class FakeRepository implements ActivityRepository{
  final ActivityModels.Activity activity;final Map<String,String> claims=new HashMap<>();final Set<String> events=new HashSet<>();long progress;LocalDate day;int streak;
  FakeRepository(ActivityModels.Activity a){activity=a;}public List<ActivityModels.Activity> active(Instant now){return !now.isBefore(activity.startsAt())&&now.isBefore(activity.endsAt())?List.of(activity):List.of();}public Optional<ActivityModels.Activity> find(String c,long v){return Optional.of(activity);}
  public ActivityModels.CheckIn checkIn(long p,ActivityModels.Activity a,LocalDate d,ActivityModels.Reward r){String key="checkin:"+a.code()+":"+a.version()+":"+p+":"+d;if(!claims.containsKey(key)){streak=day!=null&&day.plusDays(1).equals(d)?streak+1:1;day=d;claims.put(key,"RESERVED");}return new ActivityModels.CheckIn(d,streak,r,claims.get(key));}
  public ActivityModels.Progress addProgress(long p,ActivityModels.Activity a,String m,String e,long delta){if(events.add(e))progress=Math.min(3,progress+delta);String key="mission:"+a.code()+":"+a.version()+":"+p+":"+m;return new ActivityModels.Progress(a.code(),a.version(),m,progress,3,progress>=3,"COMPLETED".equals(claims.get(key)));}
  public ActivityModels.Claim reserveMissionClaim(long p,ActivityModels.Activity a,String m){if(progress<3)throw new IllegalStateException();String key="mission:"+a.code()+":"+a.version()+":"+p+":"+m;claims.putIfAbsent(key,"RESERVED");return new ActivityModels.Claim(key,claims.get(key),a.missions().get(1).reward());}
  public void completeClaim(String key){claims.put(key,"COMPLETED");}public void failClaim(String key,String reason){claims.put(key,"FAILED");}
 }
}
