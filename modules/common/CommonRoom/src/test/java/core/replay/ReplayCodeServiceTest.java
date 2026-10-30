package core.replay;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class ReplayCodeServiceTest {
    @Test void generatedCodesAreNumericAndFixedWidth(){
        var generator=new ShortReplayCodeGenerator(new Random(7));
        for(int length=6;length<=8;length++)assertTrue(generator.next(length).matches("[1-9]\\d{"+(length-1)+"}"));
    }

    @Test void allocationIsStableAndConcurrentTargetsNeverOverwrite()throws Exception{
        var repo=new MemoryRepository();var service=service(repo,0.70,64);
        ExecutorService pool=Executors.newFixedThreadPool(8);
        try{
            List<Future<ReplayCodeRepository.Mapping>> futures=new ArrayList<>();
            for(int i=0;i<32;i++){long room=1000+i;futures.add(pool.submit(()->service.allocate(room,1)));}
            Set<String> codes=new HashSet<>();for(var future:futures)assertTrue(codes.add(future.get().code()));
            var first=service.allocate(1000,1);assertEquals(first.code(),service.allocate(1000,1).code());
        }finally{pool.shutdownNow();}
    }

    @Test void thresholdPromotesToSevenAndThenEightDigits(){
        var repo=new MemoryRepository();repo.counts.put(6,(long)(ShortReplayCodeGenerator.capacity(6)*0.70));
        var service=service(repo,0.70,4);assertEquals(7,service.allocate(9,1).code().length());
        repo=new MemoryRepository();repo.counts.put(6,900_000L);repo.counts.put(7,9_000_000L);
        assertEquals(8,service(repo,0.70,4).allocate(10,1).code().length());
    }

    @Test void lookupHasExplicitTypeCompleteErrorsAndShareGrant(){
        var repo=new MemoryRepository();var service=service(repo,0.70,4);var mapping=service.allocate(22,3);
        assertEquals(ReplayCodeService.ResolutionType.SHORT,service.resolve(88,ReplayCodeService.LookupType.REPLAY_CODE,mapping.code()).type());
        assertTrue(repo.mayView(88,22,3));
        repo.legacy.put("12345678901",new ReplayCodeRepository.LegacyTarget("12345678901",31,2,7));
        assertEquals(ReplayCodeService.ResolutionType.LEGACY_11,service.resolve(88,ReplayCodeService.LookupType.REPLAY_CODE,"12345678901").type());
        assertEquals("10000",service.resolve(10000,ReplayCodeService.LookupType.PLAYER_ID,"10000").playerId());
        assertCode("REPLAY_CODE_FORMAT_INVALID",()->service.resolve(88,ReplayCodeService.LookupType.REPLAY_CODE,"12345"));
        assertCode("REPLAY_CODE_NOT_FOUND",()->service.resolve(88,ReplayCodeService.LookupType.REPLAY_CODE,"123456"));
        assertCode("REPLAY_CODE_ACCESS_DENIED",()->service.resolve(10000,ReplayCodeService.LookupType.PLAYER_ID,"10001"));
    }

    @Test void notReadyAndExpiredAreDistinct(){
        var repo=new MemoryRepository();repo.ready=false;var service=service(repo,0.70,2);
        assertCode("REPLAY_NOT_READY",()->service.allocate(1,1));
        repo.ready=true;repo.byCode.put("123456",new ReplayCodeRepository.Mapping("123456",1,1,"EXPIRED",Instant.parse("2026-01-01T00:00:00Z")));
        assertCode("REPLAY_CODE_EXPIRED",()->service.resolve(9,ReplayCodeService.LookupType.REPLAY_CODE,"123456"));
    }

    private static ReplayCodeService service(MemoryRepository repo,double threshold,int retries){return new ReplayCodeService(repo,new ShortReplayCodeGenerator(new Random(19)),new ShortReplayCodePolicy(threshold,retries),Clock.fixed(Instant.parse("2026-09-07T00:00:00Z"),ZoneOffset.UTC));}
    private static void assertCode(String expected,Runnable call){var error=assertThrows(ReplayCodeException.class,call::run);assertEquals(expected,error.code());}

    private static final class MemoryRepository implements ReplayCodeRepository{
        final Map<String,Mapping>byCode=new ConcurrentHashMap<>();final Map<String,Mapping>byTarget=new ConcurrentHashMap<>();final Map<String,LegacyTarget>legacy=new HashMap<>();final Map<Integer,Long>counts=new ConcurrentHashMap<>();final Set<String>grants=ConcurrentHashMap.newKeySet();volatile boolean ready=true;
        public Optional<Mapping>findShortCode(String code){return Optional.ofNullable(byCode.get(code));}public Optional<Mapping>findByTarget(long room,int set){return Optional.ofNullable(byTarget.get(room+":"+set));}public Optional<LegacyTarget>findLegacyCode(String code){return Optional.ofNullable(legacy.get(code));}public boolean replayReady(long room,int set){return ready;}public boolean mayView(long player,long room,int set){return grants.contains(player+":"+room+":"+set);}public void grantCodeAccess(String code,long player){var m=byCode.get(code);grants.add(player+":"+m.roomId()+":"+m.setId());}public long allocatedCount(int length){return counts.getOrDefault(length,byCode.values().stream().filter(v->v.code().length()==length).count());}
        public synchronized InsertResult insert(String code,long room,int set){String target=room+":"+set;if(byTarget.containsKey(target))return InsertResult.TARGET_EXISTS;if(byCode.containsKey(code))return InsertResult.CODE_COLLISION;var m=new Mapping(code,room,set,"ACTIVE",null);byCode.put(code,m);byTarget.put(target,m);return InsertResult.INSERTED;}
    }
}
