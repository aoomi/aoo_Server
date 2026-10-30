package com.aoo.bcg.poker.cx;

import com.aoo.bcg.gamespi.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CxGameProvider implements GameProvider {
    private static final String DISPATCH="poker.chengdu2999.dispatch";
    private static final Set<String> COMMANDS=Set.of(
            "poker.chengdu2999.state_req","poker.chengdu2999.sit_req","poker.chengdu2999.ready_req","poker.chengdu2999.preset_req",
            "poker.chengdu2999.bet_req","poker.chengdu2999.add_card_req","poker.chengdu2999.split_req","poker.chengdu2999.delay_split_req",
            "poker.chengdu2999.continue_req","poker.chengdu2999.restart_req","poker.chengdu2999.rebuy_req","poker.chengdu2999.stand_req","poker.chengdu2999.timeout_req");
    private final GameDescriptor descriptor;
    public CxGameProvider(GameDescriptor descriptor){this.descriptor=descriptor;}
    public GameDescriptor descriptor(){return descriptor;}
    public GameRoomFactory roomFactory(){return c->new GameRoomHandle(c.roomId(),descriptor.gameId(),descriptor.version(),create(c),null,c.snapshotMetadataSource());}
    public Optional<AuthoritativeGameSession>createAuthoritativeSession(RoomCreationContext c){return Optional.of(create(c));}
    public Optional<GameCommandHandler>commandHandler(){var authoritative=new AuthoritativeSessionCommandHandler();return Optional.of((room,request)->authoritative.handle(room,unwrap(request)));}
    public Optional<ReconnectViewProvider<?>>reconnectViewProvider(){return Optional.of((p,r)->r.requireAuthoritativeSession().viewFor(p));}
    public Optional<SettlementProvider>settlementProvider(){return Optional.of((r,n)->r.requireAuthoritativeSession().settlement(n,descriptor.version()));}
    public Optional<AuthoritativeGameSession>restoreAuthoritativeSession(Map<String,Object>state){return Optional.of(CxSession.restore(state));}
    public Optional<EventReplayProvider>eventReplayProvider(){return Optional.of(CxSession::replay);}
    public List<RuleComponent<GameCommandRequest>>ruleComponents(){return List.of(new RuleComponent<>(){
        public String ruleId(){return"cx.command-boundary";}
        public String componentVersion(){return CxRules.VERSION;}
        public RuleStage stage(){return RuleStage.PLAY;}
        public int priority(){return 100;}
        public RuleResult execute(GameCommandRequest request){
            if(!CxRules.GAME_CODE.equals(descriptor.code())||!CxRules.FAMILY.equals(descriptor.family()))
                return RuleResult.reject("CX_IDENTITY_MISMATCH","[CX] provider identity mismatch code="+descriptor.code()+" family="+descriptor.family());
            if(!CxRules.VERSION.equals(descriptor.version())||!CxRules.VERSION.equals(request.playVersion()))
                return RuleResult.reject("CX_PLAY_VERSION_MISMATCH","[CX] playVersion mismatch provider="+descriptor.version()+" request="+request.playVersion());
            String command=DISPATCH.equals(request.msgId())?String.valueOf(request.body().asMap().get("action")):request.msgId();
            if(!COMMANDS.contains(command))
                return RuleResult.reject("CX_COMMAND_NOT_ALLOWED","[CX] msgId="+request.msgId());
            return RuleResult.accept();
        }
    });}
    public Map<String,Object>defaultConfiguration(){var config=new java.util.LinkedHashMap<String,Object>();config.put("enabled",true);config.put("gameCode",CxRules.GAME_CODE);config.put("family",CxRules.FAMILY);config.put("playVersion",descriptor.version());config.put("provider","cx-native");config.putAll(CxRules.from(Map.of()).toMap());return Map.copyOf(config);}
    private CxSession create(RoomCreationContext c){return new CxSession(c.roomId(),c.ownerId(),c.roomId(),CxRules.from(c.immutableRules().asMap()));}
    private static GameCommandRequest unwrap(GameCommandRequest request){if(!DISPATCH.equals(request.msgId()))return request;Map<String,Object>body=request.body().asMap();String action=String.valueOf(body.get("action"));if(!COMMANDS.contains(action))throw new IllegalArgumentException("CX_COMMAND_NOT_ALLOWED: [CX] action="+action);Object rawPayload=body.get("payload");if(!(rawPayload instanceof Map<?,?>raw))throw new IllegalArgumentException("[CX] dispatch payload must be an object");Map<String,Object>payload=new java.util.LinkedHashMap<>();raw.forEach((key,value)->payload.put(String.valueOf(key),value));return new GameCommandRequest(action,request.requestId(),request.sequence(),request.roomId(),request.roundNo(),request.playVersion(),request.authenticatedUserId(),request.seatId(),payload);}
}
