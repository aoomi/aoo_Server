package com.aoo.bcg.poker.cd299;

import com.aoo.bcg.gamespi.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Cd299GameProvider implements GameProvider {
    private static final Set<String> COMMANDS=Set.of(
            "poker.cd299.state_req","poker.cd299.sit_req","poker.cd299.ready_req","poker.cd299.preset_req",
            "poker.cd299.bet_req","poker.cd299.add_card_req","poker.cd299.split_req",
            "poker.cd299.continue_req","poker.cd299.timeout_req");
    private final GameDescriptor descriptor;
    public Cd299GameProvider(GameDescriptor descriptor){this.descriptor=descriptor;}
    public GameDescriptor descriptor(){return descriptor;}
    public GameRoomFactory roomFactory(){return c->new GameRoomHandle(c.roomId(),descriptor.gameId(),descriptor.version(),create(c));}
    public Optional<AuthoritativeGameSession>createAuthoritativeSession(RoomCreationContext c){return Optional.of(create(c));}
    public Optional<GameCommandHandler>commandHandler(){return Optional.of(new AuthoritativeSessionCommandHandler());}
    public Optional<ReconnectViewProvider<?>>reconnectViewProvider(){return Optional.of((p,r)->r.requireAuthoritativeSession().viewFor(p));}
    public Optional<SettlementProvider>settlementProvider(){return Optional.of((r,n)->r.requireAuthoritativeSession().settlement(n,descriptor.version()));}
    public Optional<AuthoritativeGameSession>restoreAuthoritativeSession(Map<String,Object>state){return Optional.of(Cd299Session.restore(state));}
    public Optional<EventReplayProvider>eventReplayProvider(){return Optional.of(Cd299Session::replay);}
    public List<RuleComponent<GameCommandRequest>>ruleComponents(){return List.of(new RuleComponent<>(){
        public String ruleId(){return"cd299.command-boundary";}
        public String componentVersion(){return Cd299Rules.VERSION;}
        public RuleStage stage(){return RuleStage.PLAY;}
        public int priority(){return 100;}
        public RuleResult execute(GameCommandRequest request){
            if(!Cd299Rules.GAME_CODE.equals(descriptor.code())||!Cd299Rules.FAMILY.equals(descriptor.family()))
                return RuleResult.reject("CD299_IDENTITY_MISMATCH","[CD299] provider identity mismatch code="+descriptor.code()+" family="+descriptor.family());
            if(!Cd299Rules.VERSION.equals(descriptor.version())||!Cd299Rules.VERSION.equals(request.playVersion()))
                return RuleResult.reject("CD299_PLAY_VERSION_MISMATCH","[CD299] playVersion mismatch provider="+descriptor.version()+" request="+request.playVersion());
            if(!COMMANDS.contains(request.msgId()))
                return RuleResult.reject("CD299_COMMAND_NOT_ALLOWED","[CD299] msgId="+request.msgId());
            return RuleResult.accept();
        }
    });}
    public Map<String,Object>defaultConfiguration(){var config=new java.util.LinkedHashMap<String,Object>();config.put("enabled",true);config.put("gameCode",Cd299Rules.GAME_CODE);config.put("family",Cd299Rules.FAMILY);config.put("playVersion",descriptor.version());config.put("provider","cd299-native");config.putAll(Cd299Rules.from(Map.of()).toMap());return Map.copyOf(config);}
    private Cd299Session create(RoomCreationContext c){return new Cd299Session(c.roomId(),c.ownerId(),c.roomId(),Cd299Rules.from(c.immutableRules().asMap()));}
}
