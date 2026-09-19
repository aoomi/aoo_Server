package com.aoo.bcg.poker.nn;

import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.AuthoritativeSessionCommandHandler;
import com.aoo.bcg.gamespi.CommandPayload;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.GameCommandHandler;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomFactory;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.ReconnectViewProvider;
import com.aoo.bcg.gamespi.RuleComponent;
import com.aoo.bcg.gamespi.RuleResult;
import com.aoo.bcg.gamespi.RuleStage;
import com.aoo.bcg.gamespi.SettlementPayload;
import com.aoo.bcg.gamespi.SettlementProvider;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class CN298GameProvider implements GameProvider {
  private static final String DISPATCH = "poker.CN298.dispatch";
  private static final Set<String> COMMANDS = Set.of(
      "poker.cn298.sit_req", "poker.cn298.start_req", "poker.cn298.rob_req",
      "poker.cn298.bet_req", "poker.cn298.split_req", "poker.cn298.timeout_req",
      "poker.cn298.continue_req", "poker.cn298.state_req");
  private final GameDescriptor descriptor;
  CN298GameProvider(GameDescriptor descriptor) { this.descriptor = descriptor; }
  @Override public GameDescriptor descriptor() { return descriptor; }
  @Override public Optional<GameCommandHandler> commandHandler() {
    var authoritative = new AuthoritativeSessionCommandHandler();
    return Optional.of((room, request) -> authoritative.handle(room, unwrap(request)));
  }
  @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider() {
    return Optional.of((playerId, room) -> room.requireAuthoritativeSession().viewFor(playerId));
  }
  @Override public Optional<SettlementProvider> settlementProvider() {
    return Optional.of((room, roundNo) ->
        room.requireAuthoritativeSession().settlement(roundNo, descriptor.version()));
  }
  @Override public List<RuleComponent<GameCommandRequest>> ruleComponents() {
    return List.of(new RuleComponent<>() {
      @Override public String ruleId() { return "cn298.command-boundary"; }
      @Override public String componentVersion() { return NiuNiuRules.PLAY_VERSION; }
      @Override public RuleStage stage() { return RuleStage.PLAY; }
      @Override public int priority() { return 100; }
      @Override public RuleResult execute(GameCommandRequest request) {
        if (!NiuNiuRules.GAME_CODE.equals(descriptor.code())
            || !NiuNiuRules.FAMILY.equals(descriptor.family())) {
          return RuleResult.reject("CN298_IDENTITY_MISMATCH",
              "[CN298] provider identity mismatch code=" + descriptor.code()
                  + " family=" + descriptor.family());
        }
        if (!NiuNiuRules.PLAY_VERSION.equals(request.playVersion())) {
          return RuleResult.reject("CN298_PLAY_VERSION_MISMATCH",
              "[CN298] playVersion=" + request.playVersion());
        }
        String command = DISPATCH.equals(request.msgId())
            ? String.valueOf(request.body().asMap().get("action")) : request.msgId();
        if (!COMMANDS.contains(command)) {
          return RuleResult.reject("CN298_COMMAND_NOT_ALLOWED",
              "[CN298] msgId=" + request.msgId());
        }
        return RuleResult.accept();
      }
    });
  }
  @Override public GameRoomFactory roomFactory() {
    return context -> new GameRoomHandle(context.roomId(), descriptor.gameId(), descriptor.version(), create(context));
  }
  @Override public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) {
    return Optional.of(create(context));
  }
  @Override public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String, Object> state) {
    return Optional.of(new CN298Authority(NiuNiuSession.restore(state), descriptor.version()));
  }
  @Override public Map<String, Object> defaultConfiguration() {
    NiuNiuRules rules = NiuNiuRules.defaults();
    return Map.of("enabled", true, "provider", "cn298-niuniu", "gameCode", NiuNiuRules.GAME_CODE,
        "playVersion", descriptor.version(), "rounds", rules.rounds(), "maxPlayers", rules.maxPlayers(),
        "startPlayers", rules.startPlayers(), "mode", rules.mode().name());
  }

  private CN298Authority create(RoomCreationContext context) {
    Map<String, Object> raw = context.immutableRules().asMap();
    NiuNiuRules d = NiuNiuRules.defaults();
    NiuNiuRules rules = new NiuNiuRules(number(raw,"rounds",d.rounds()), number(raw,"maxPlayers",d.maxPlayers()),
        number(raw,"startPlayers",d.startPlayers()), enumValue(NiuNiuRules.Mode.class,raw.get("mode"),d.mode()),
        number(raw,"maxRobMultiplier",d.maxRobMultiplier()), number(raw,"maxPushMultiplier",d.maxPushMultiplier()),
        enumValue(NiuNiuRules.StandPolicy.class,raw.get("standPolicy"),d.standPolicy()),
        raw.get("fastModeEnabled") instanceof Boolean b ? b : d.fastModeEnabled(),
        raw.get("kanShunDouEnabled") instanceof Boolean b ? b : d.kanShunDouEnabled());
    long seed = raw.get("shuffleSeed") instanceof Number n ? n.longValue() : context.roomId();
    return new CN298Authority(new NiuNiuSession(context.roomId(), context.ownerId(), seed, rules), descriptor.version());
  }
  private static int number(Map<String,Object> values,String key,int fallback){Object v=values.get(key);return v instanceof Number n?n.intValue():fallback;}
  private static <E extends Enum<E>> E enumValue(Class<E> type,Object raw,E fallback){
    if(raw==null)return fallback;return Enum.valueOf(type,String.valueOf(raw).trim().toUpperCase());
  }
  private static GameCommandRequest unwrap(GameCommandRequest request) {
    if (!DISPATCH.equals(request.msgId())) return request;
    Map<String, Object> body = request.body().asMap();
    String action = String.valueOf(body.get("action"));
    if (!COMMANDS.contains(action)) {
      throw new IllegalArgumentException("CN298_COMMAND_NOT_ALLOWED: [CN298] action=" + action);
    }
    Object rawPayload = body.get("payload");
    if (!(rawPayload instanceof Map<?, ?> raw)) {
      throw new IllegalArgumentException("[CN298] dispatch payload must be an object");
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    raw.forEach((key, value) -> payload.put(String.valueOf(key), value));
    return new GameCommandRequest(action, request.requestId(), request.sequence(), request.roomId(),
        request.roundNo(), request.playVersion(), request.authenticatedUserId(), request.seatId(), payload);
  }
}

final class CN298Authority implements AuthoritativeGameSession {
  private final NiuNiuSession session;
  private final String version;
  private final OperationDeadlineArbiter arbiter = new OperationDeadlineArbiter();
  CN298Authority(NiuNiuSession session, String version) { this.session=session; this.version=version; }
  @Override public synchronized GameCommandResult execute(GameCommandRequest request) {
    if (request.roomId() != session.roomId()) {
      throw new IllegalArgumentException("CN298 request roomId does not match authoritative room");
    }
    if (!version.equals(request.playVersion())) {
      throw new IllegalArgumentException("CN298 request playVersion does not match room version");
    }
    CommandPayload body=request.body(); int seat=request.seatId();
    long playerId=player(request.authenticatedUserId());
    if (!Set.of("poker.cn298.sit_req", "poker.cn298.state_req").contains(request.msgId()))
      requireSeatOwner(seat, playerId);
    boolean diagnostic = !"poker.cn298.state_req".equals(request.msgId());
    if (diagnostic) System.out.printf("[CN298] command roomId=%d playerId=%d operationId=%s stateVersion=%d action=%s%n",
        request.roomId(), playerId, request.requestId(), session.stateVersion(), request.msgId());
    try {
      switch(request.msgId()){
        case "poker.cn298.sit_req"->session.sit(body.requireInt("seatId"),playerId,request.requestId());
        case "poker.cn298.start_req"->session.start(playerId,request.requestId());
        case "poker.cn298.rob_req"->session.rob(seat,body.requireInt("multiplier"),request.requestId());
        case "poker.cn298.bet_req"->session.bet(seat,body.requireInt("multiplier"),request.requestId());
        case "poker.cn298.split_req"->session.split(seat,body.requireIntList("cards"),request.requestId());
        case "poker.cn298.timeout_req"->session.timeout(seat,request.requestId());
        case "poker.cn298.continue_req"->session.continueNextRound(seat,request.requestId());
        case "poker.cn298.state_req"->{}
        default->throw new IllegalArgumentException("unsupported CN298 command: "+request.msgId());
      }
    } catch (RuntimeException failure) {
      if (diagnostic) System.err.printf("[CN298] command failed roomId=%d playerId=%d operationId=%s stateVersion=%d action=%s reason=%s%n",
          request.roomId(), playerId, request.requestId(), session.stateVersion(), request.msgId(), failure.getMessage());
      throw failure;
    }
    if (diagnostic) System.out.printf("[CN298] command accepted roomId=%d playerId=%d operationId=%s stateVersion=%d action=%s%n",
        request.roomId(), playerId, request.requestId(), session.stateVersion(), request.msgId());
    return new GameCommandResult(request.msgId().replace("_req","_resp"),request.requestId(),viewFor(playerId));
  }
  @Override public Map<String,Object> viewFor(long viewerPlayerId){return session.snapshotFor(viewerPlayerId);}
  @Override public Map<String,Object> authoritativeState(){return session.authoritativeState();}
  @Override public long stateVersion(){return session.stateVersion();}
  @Override public OperationDeadline operationDeadline(){return OperationDeadline.none();}
  @Override public OperationDeadlineArbiter deadlineArbiter(){return arbiter;}
  @Override public List<String> invariantViolations(){return List.of();}
  @Override public SettlementPayload settlement(int roundNo,String playVersion){
    var result=session.lastResult(); if(result==null||result.round()!=roundNo)throw new IllegalStateException("CN298 round is not settled");
    Map<Long,Long> delta=new LinkedHashMap<>();
    result.scoreDelta().forEach((seat,score)->delta.put(session.players().get(seat),score));
    return new SettlementPayload(session.roomId(),roundNo,playVersion,delta);
  }
  private static long player(String id){try{return Long.parseLong(id);}catch(NumberFormatException e){throw new IllegalArgumentException("CN298 numeric player id required",e);}}
  private void requireSeatOwner(int seat, long playerId) {
    if (!Long.valueOf(playerId).equals(session.players().get(seat))) {
      throw new IllegalArgumentException("CN298 authenticated player does not own requested seat");
    }
  }
}
