package com.aoo.bcg.mahjong;

import com.aoo.bcg.gamespi.*;
import java.util.*;

/** Single provider implementation used by all five Mahjong gameplay-family registries. */
public final class CatalogMahjongFamilyProvider implements MahjongGameProvider {
    private final GameDescriptor descriptor;
    private final MahjongRegionRuntimeProfile profile;
    CatalogMahjongFamilyProvider(GameDescriptor descriptor, MahjongRegionRuntimeProfile profile) {
        if (descriptor.gameId()!=profile.gameId() || !descriptor.code().equals(profile.code()) || descriptor.category()!=GameCategory.MAHJONG)
            throw new IllegalArgumentException("catalog/profile mismatch");
        this.descriptor=descriptor;this.profile=profile;
    }
    public GameDescriptor descriptor(){return descriptor;}
    public MahjongRuleFamily mahjongFamily(){return new ProfiledMahjongRuleFamily(profile);}
    public GameRoomFactory roomFactory(){return context->new GameRoomHandle(context.roomId(),descriptor.gameId(),descriptor.version(),create(context));}
    public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context){return Optional.of(create(context));}
    private CatalogMahjongFamilySession create(RoomCreationContext context){return new CatalogMahjongFamilySession(context.roomId(),context.ownerId(),context.immutableRules(),profile,descriptor.version());}
    public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String,Object> state){return Optional.of(CatalogMahjongFamilySession.restore(state,profile,descriptor.version()));}
    public Optional<GameCommandHandler> commandHandler(){return Optional.of(new AuthoritativeSessionCommandHandler());}
    public Optional<ReconnectViewProvider<?>> reconnectViewProvider(){return Optional.of((viewer,room)->room.requireAuthoritativeSession().viewFor(viewer));}
    public Optional<SettlementProvider> settlementProvider(){return Optional.of((room,round)->room.requireAuthoritativeSession().settlement(round,descriptor.version()));}
    public Optional<EventReplayProvider> eventReplayProvider(){return Optional.of(CatalogMahjongFamilySession::replay);}
    public Map<String,Object> defaultConfiguration(){
        Map<String,Object> out=new LinkedHashMap<>();out.put("enabled",true);out.put("provider","native-mahjong-family");out.put("playVersion",descriptor.version());
        out.put("gameplayFamily",profile.family());out.put("runtimeArchetype",profile.archetype());out.put("regionConfig",profile.region());
        out.put("ruleComponents",profile.orderedRuleComponents());out.put("lifecycleComponents",profile.orderedLifecycleComponents());
        out.put("allowedCreateFields",profile.allowedCreateFields().stream().sorted().toList());out.put("configSchemaHash",profile.configSchemaHash());
        out.put("playerNum",4);out.put("shuffleSeed",0L);return Map.copyOf(out);
    }
    public List<RuleComponent<GameCommandRequest>> ruleComponents(){
        List<RuleComponent<GameCommandRequest>> out=new ArrayList<>();
        out.add(component("catalog-route",RuleStage.PLAY,10,request->("mahjong."+profile.code()+".dispatch").equals(request.msgId())));
        for(String id:profile.orderedRuleComponents())out.add(component("rule."+id,RuleStage.PLAY,20,request->true));
        for(String id:profile.orderedLifecycleComponents())out.add(component("lifecycle."+id,RuleStage.SETTLEMENT,30,request->true));
        return List.copyOf(out);
    }
    private RuleComponent<GameCommandRequest> component(String id,RuleStage stage,int priority,java.util.function.Predicate<GameCommandRequest> predicate){return new RuleComponent<>(){public String ruleId(){return"mahjong.family."+profile.family()+"."+id;}public String componentVersion(){return descriptor.version();}public RuleStage stage(){return stage;}public int priority(){return priority;}public RuleResult execute(GameCommandRequest request){return predicate.test(request)?RuleResult.accept():RuleResult.reject("MAHJONG_CATALOG_ROUTE_MISMATCH","wrong region route");}};}
    public GameCapabilityManifest capabilityManifest(){return new GameCapabilityManifest(Map.of(GameCapability.ROOM_SHUFFLE,"server-seeded family runtime; profile="+profile.archetype()));}
    MahjongRegionRuntimeProfile profile(){return profile;}
}
