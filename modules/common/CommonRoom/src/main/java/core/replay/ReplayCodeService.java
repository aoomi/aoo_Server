package core.replay;

import java.time.Clock;
import java.util.Objects;

/** Canonical allocation and explicit-mode lookup service. */
public final class ReplayCodeService {
    public enum LookupType { REPLAY_CODE, PLAYER_ID }
    public enum ResolutionType { SHORT, LEGACY_11 }
    private final ReplayCodeRepository repository;
    private final ShortReplayCodeGenerator generator;
    private final ShortReplayCodePolicy policy;
    private final Clock clock;
    public ReplayCodeService(ReplayCodeRepository repository) { this(repository,new ShortReplayCodeGenerator(),ShortReplayCodePolicy.configured(),Clock.systemUTC()); }
    public ReplayCodeService(ReplayCodeRepository repository,ShortReplayCodeGenerator generator,ShortReplayCodePolicy policy,Clock clock){this.repository=Objects.requireNonNull(repository);this.generator=Objects.requireNonNull(generator);this.policy=Objects.requireNonNull(policy);this.clock=Objects.requireNonNull(clock);}
    public ReplayCodeRepository.Mapping allocate(long roomId,int setId){
        if(roomId<=0||setId<0)throw error("REPLAY_CODE_FORMAT_INVALID","回放目标格式错误");
        var existing=repository.findByTarget(roomId,setId);if(existing.isPresent())return existing.get();
        if(!repository.replayReady(roomId,setId))throw error("REPLAY_NOT_READY","本小局回放尚未生成");
        for(int length=6;length<=8;length++){
            long capacity=ShortReplayCodeGenerator.capacity(length);
            if(repository.allocatedCount(length)>=Math.floor(capacity*policy.capacityThreshold()))continue;
            for(int attempt=0;attempt<policy.retriesPerLength();attempt++){
                String candidate=generator.next(length);var result=repository.insert(candidate,roomId,setId);
                if(result==ReplayCodeRepository.InsertResult.INSERTED)return repository.findShortCode(candidate).orElseThrow();
                if(result==ReplayCodeRepository.InsertResult.TARGET_EXISTS)return repository.findByTarget(roomId,setId).orElseThrow();
            }
        }
        throw error("REPLAY_CODE_CONFLICT_EXHAUSTED","回放码号码池冲突耗尽");
    }
    public Resolution resolve(long playerId,LookupType lookupType,String value){
        if(playerId<=0)throw error("REPLAY_CODE_ACCESS_DENIED","需要登录后查看回放");
        if(lookupType==LookupType.PLAYER_ID){
            if(!value.matches("[1-9]\\d{4,}"))throw error("PLAYER_ID_FORMAT_INVALID","用户ID须为至少5位纯数字");
            if(!value.equals(Long.toString(playerId)))throw error("REPLAY_CODE_ACCESS_DENIED","无权查看该玩家战绩");
            return new Resolution(null,null,0,0,null,value);
        }
        if(!value.matches("(?:\\d{6}|\\d{7}|\\d{8}|\\d{11})"))throw error("REPLAY_CODE_FORMAT_INVALID","回放码须为6、7、8或11位纯数字");
        if(value.length()==11){var legacy=repository.findLegacyCode(value).orElseThrow(()->error("REPLAY_CODE_NOT_FOUND","回放码不存在"));return new Resolution(ResolutionType.LEGACY_11,value,legacy.roomId(),legacy.setId(),legacy.gameType(),null);}
        var mapping=repository.findShortCode(value).orElseThrow(()->error("REPLAY_CODE_NOT_FOUND","回放码不存在"));
        if(!"ACTIVE".equals(mapping.status())||(mapping.expiresAt()!=null&&!mapping.expiresAt().isAfter(clock.instant())))throw error("REPLAY_CODE_EXPIRED","回放码已过期");
        if(!repository.replayReady(mapping.roomId(),mapping.setId()))throw error("REPLAY_NOT_READY","本小局回放尚未生成");
        repository.grantCodeAccess(value,playerId);
        return new Resolution(ResolutionType.SHORT,value,mapping.roomId(),mapping.setId(),null,null);
    }
    public ReplayCodeRepository.Mapping current(long playerId,long roomId,int setId){
        if(playerId<=0||!repository.mayView(playerId,roomId,setId))throw error("REPLAY_CODE_ACCESS_DENIED","无权查看该小局回放码");
        return allocate(roomId,setId);
    }
    private static ReplayCodeException error(String code,String message){return new ReplayCodeException(code,message);}
    public record Resolution(ResolutionType type,String replayCode,long roomId,int setId,Integer legacyGameType,String playerId){}
}
