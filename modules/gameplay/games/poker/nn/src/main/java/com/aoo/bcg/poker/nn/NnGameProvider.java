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
import com.aoo.bcg.gamespi.RoomScoreLedger;
import com.aoo.bcg.gamespi.RoomScoreLedgerAware;
import com.aoo.bcg.gamespi.ParticipantProfileAware;
import com.aoo.bcg.gamespi.ParticipantProfileSource;
import com.aoo.bcg.gamespi.ReconnectViewProvider;
import com.aoo.bcg.gamespi.RuleComponent;
import com.aoo.bcg.gamespi.RuleResult;
import com.aoo.bcg.gamespi.RuleStage;
import com.aoo.bcg.gamespi.SettlementPayload;
import com.aoo.bcg.gamespi.SettlementProvider;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class NnGameProvider implements GameProvider, RoomScoreLedgerAware, ParticipantProfileAware {
  private static final String DISPATCH = "poker.quanguo2998.dispatch";
  private static final Set<String> COMMANDS = Set.of(
      "poker.quanguo2998.sit_req", "poker.quanguo2998.start_req", "poker.quanguo2998.rob_req",
      "poker.quanguo2998.bet_req", "poker.quanguo2998.split_req", "poker.quanguo2998.timeout_req",
      "poker.quanguo2998.continue_req", "poker.quanguo2998.state_req", "common.room.leave_req");
  private final GameDescriptor descriptor;
  private final Set<NnAuthority> authorities = ConcurrentHashMap.newKeySet();
  private volatile RoomScoreLedger roomScoreLedger;
  private volatile ParticipantProfileSource participantProfileSource;
  NnGameProvider(GameDescriptor descriptor) { this.descriptor = descriptor; }
  @Override public synchronized void attachRoomScoreLedger(RoomScoreLedger ledger) {
    Objects.requireNonNull(ledger, "ledger");
    if (roomScoreLedger == ledger) return;
    if (roomScoreLedger != null) throw new IllegalStateException("NN room score ledger already attached");
    roomScoreLedger = ledger;
    authorities.forEach(authority -> authority.attachRoomScoreLedger(ledger));
  }
  @Override public synchronized void attachParticipantProfileSource(ParticipantProfileSource source) {
    Objects.requireNonNull(source, "source");
    if (participantProfileSource == source) return;
    if (participantProfileSource != null) throw new IllegalStateException("NN participant profile source already attached");
    participantProfileSource = source;
    authorities.forEach(authority -> authority.attachParticipantProfileSource(source));
  }
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
      @Override public String ruleId() { return "nn.command-boundary"; }
      @Override public String componentVersion() { return NnRules.PLAY_VERSION; }
      @Override public RuleStage stage() { return RuleStage.PLAY; }
      @Override public int priority() { return 100; }
      @Override public RuleResult execute(GameCommandRequest request) {
        if (!NnRules.GAME_CODE.equals(descriptor.code())
            || !NnRules.FAMILY.equals(descriptor.family())) {
          return RuleResult.reject("NN_IDENTITY_MISMATCH",
              "[NN] provider identity mismatch code=" + descriptor.code()
                  + " family=" + descriptor.family());
        }
        if (!NnRules.PLAY_VERSION.equals(request.playVersion())) {
          return RuleResult.reject("NN_PLAY_VERSION_MISMATCH",
              "[NN] playVersion=" + request.playVersion());
        }
        String command = DISPATCH.equals(request.msgId())
            ? String.valueOf(request.body().asMap().get("action")) : request.msgId();
        if (!COMMANDS.contains(command)) {
          return RuleResult.reject("NN_COMMAND_NOT_ALLOWED",
              "[NN] msgId=" + request.msgId());
        }
        return RuleResult.accept();
      }
    });
  }
  @Override public GameRoomFactory roomFactory() {
    return context -> new GameRoomHandle(context.roomId(), descriptor.gameId(), descriptor.version(), create(context),
        null, context.snapshotMetadataSource());
  }
  @Override public Optional<AuthoritativeGameSession> createAuthoritativeSession(RoomCreationContext context) {
    return Optional.of(create(context));
  }
  @Override public Optional<AuthoritativeGameSession> restoreAuthoritativeSession(Map<String, Object> state) {
    return Optional.of(register(new NnAuthority(NnSession.restore(state), descriptor.version(), 0)));
  }
  @Override public Map<String, Object> defaultConfiguration() {
    NnRules rules = NnRules.defaults();
    return Map.ofEntries(Map.entry("enabled", true), Map.entry("provider", "nn-native"),
        Map.entry("gameCode", NnRules.GAME_CODE), Map.entry("playVersion", descriptor.version()),
        Map.entry("rounds", rules.rounds()), Map.entry("maxPlayers", rules.maxPlayers()),
        Map.entry("startPlayers", rules.startPlayers()), Map.entry("mode", rules.mode().name()),
        Map.entry("baseScore", rules.baseScore()), Map.entry("enterScore", rules.enterScore()),
        Map.entry("leaveScore", rules.leaveScore()));
  }

  private NnAuthority create(RoomCreationContext context) {
    Map<String, Object> raw = context.immutableRules().asMap();
    NnRules d = NnRules.defaults();
    NnRules rules = new NnRules(number(raw,"rounds",d.rounds()), number(raw,"maxPlayers",d.maxPlayers()),
        number(raw,"startPlayers",d.startPlayers()), enumValue(NnRules.Mode.class,raw.get("mode"),d.mode()),
        number(raw,"maxRobMultiplier",d.maxRobMultiplier()), number(raw,"maxPushMultiplier",d.maxPushMultiplier()),
        enumValue(NnRules.StandPolicy.class,raw.get("standPolicy"),d.standPolicy()),
        raw.get("fastModeEnabled") instanceof Boolean b ? b : d.fastModeEnabled(),
        raw.get("kanShunDouEnabled") instanceof Boolean b ? b : d.kanShunDouEnabled(),
        longNumber(raw,"baseScore",NnRules.BASE_SCORE),
        longNumber(raw,"enterScore",NnRules.ENTER_SCORE),
        longNumber(raw,"leaveScore",NnRules.LEAVE_SCORE));
    long seed = raw.get("shuffleSeed") instanceof Number n ? n.longValue() : context.roomId();
    // 规则表无此字段，冲突登记见 evidence/AOO-0042/09；未配置时保持 0（无门槛）。
    long minCarryToRob = longNumber(raw, "minCarryToRob", 0);
    return register(new NnAuthority(new NnSession(context.roomId(), context.ownerId(), seed, rules),
        descriptor.version(), minCarryToRob));
  }
  private static int number(Map<String,Object> values,String key,int fallback){Object v=values.get(key);return v instanceof Number n?n.intValue():fallback;}
  private static long longNumber(Map<String,Object> values,String key,long fallback){Object v=values.get(key);return v instanceof Number n?n.longValue():fallback;}
  private NnAuthority register(NnAuthority authority) {
    authorities.add(authority);
    RoomScoreLedger ledger = roomScoreLedger;
    if (ledger != null) authority.attachRoomScoreLedger(ledger);
    ParticipantProfileSource source = participantProfileSource;
    if (source != null) authority.attachParticipantProfileSource(source);
    return authority;
  }
  private static <E extends Enum<E>> E enumValue(Class<E> type,Object raw,E fallback){
    if(raw==null)return fallback;return Enum.valueOf(type,String.valueOf(raw).trim().toUpperCase());
  }
  private static GameCommandRequest unwrap(GameCommandRequest request) {
    if (!DISPATCH.equals(request.msgId())) return request;
    Map<String, Object> body = request.body().asMap();
    String action = String.valueOf(body.get("action"));
    if (!COMMANDS.contains(action)) {
      throw new IllegalArgumentException("NN_COMMAND_NOT_ALLOWED: [NN] action=" + action);
    }
    Object rawPayload = body.get("payload");
    if (!(rawPayload instanceof Map<?, ?> raw)) {
      throw new IllegalArgumentException("[NN] dispatch payload must be an object");
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    raw.forEach((key, value) -> payload.put(String.valueOf(key), value));
    return new GameCommandRequest(action, request.requestId(), request.sequence(), request.roomId(),
        request.roundNo(), request.playVersion(), request.authenticatedUserId(), request.seatId(), payload);
  }
}

final class NnAuthority implements AuthoritativeGameSession, RoomScoreLedgerAware, ParticipantProfileAware {
  private final NnSession session;
  private final String version;
  private final long minCarryToRob;
  private final OperationDeadlineArbiter arbiter = new OperationDeadlineArbiter();
  private volatile RoomScoreLedger roomScoreLedger;
  private volatile ParticipantProfileSource participantProfileSource;
  NnAuthority(NnSession session, String version) { this(session, version, 0); }
  NnAuthority(NnSession session, String version, long minCarryToRob) {
    if (minCarryToRob < 0) throw new IllegalArgumentException("NN minCarryToRob must not be negative");
    this.session=session; this.version=version; this.minCarryToRob=minCarryToRob;
    session.setRobMinCarryScore(minCarryToRob);
  }
  @Override public synchronized void attachRoomScoreLedger(RoomScoreLedger ledger) {
    Objects.requireNonNull(ledger, "ledger");
    if (roomScoreLedger == ledger) return;
    if (roomScoreLedger != null) throw new IllegalStateException("NN room score ledger already attached");
    roomScoreLedger = ledger;
    var port = new NnScoreSettlementPort(ledger);
    session.setCarryQuery(seat -> port.capture(session.roomId(), session.players()).carryBySeat().get((int) seat));
    session.setRobMinCarryScore(minCarryToRob);
  }
  @Override public synchronized void attachParticipantProfileSource(ParticipantProfileSource source) {
    Objects.requireNonNull(source, "source");
    if (participantProfileSource == source) return;
    if (participantProfileSource != null) throw new IllegalStateException("NN participant profile source already attached");
    participantProfileSource = source;
    session.players().forEach((seat, playerId) -> resolvePlayerName(source, seat, playerId));
  }
  @Override public synchronized GameCommandResult execute(GameCommandRequest request) {
    if (request.roomId() != session.roomId()) {
      throw new IllegalArgumentException("NN request roomId does not match authoritative room");
    }
    if (!version.equals(request.playVersion())) {
      throw new IllegalArgumentException("NN request playVersion does not match room version");
    }
    CommandPayload body=request.body(); int seat=request.seatId();
    long playerId=player(request.authenticatedUserId());
    if (!Set.of("poker.quanguo2998.sit_req", "poker.quanguo2998.state_req", "common.room.leave_req").contains(request.msgId()))
      requireSeatOwner(seat, playerId);
    boolean diagnostic = !"poker.quanguo2998.state_req".equals(request.msgId());
    if (diagnostic) System.out.printf("[NN] command roomId=%d playerId=%d operationId=%s stateVersion=%d action=%s%n",
        request.roomId(), playerId, request.requestId(), session.stateVersion(), request.msgId());
    try {
      switch(request.msgId()){
        case "poker.quanguo2998.sit_req"->{
          int assignedSeat=session.sit(playerId,request.requestId());
          ParticipantProfileSource source=participantProfileSource;
          if(source!=null&&!session.hasPlayerName(assignedSeat))resolvePlayerName(source,assignedSeat,playerId);
        }
        case "poker.quanguo2998.start_req"->session.start(playerId,request.requestId());
        case "poker.quanguo2998.rob_req"->session.rob(seat,body.requireInt("multiplier"),request.requestId());
        case "poker.quanguo2998.bet_req"->session.bet(seat,body.requireInt("multiplier"),request.requestId());
        case "poker.quanguo2998.split_req"->session.split(seat,body.requireIntList("cards"),request.requestId());
        case "poker.quanguo2998.timeout_req"->session.timeout(seat,request.requestId());
        case "poker.quanguo2998.continue_req"->session.continueNextRound(seat,request.requestId());
        case "poker.quanguo2998.state_req"->{
          ParticipantProfileSource source=participantProfileSource;
          String nickname=source==null?"玩家"+playerId:source.nickname(playerId);
          session.observe(playerId,nickname);
        }
        case "common.room.leave_req"->{
          int ownedSeat=session.players().entrySet().stream().filter(entry->entry.getValue()==playerId)
              .map(Map.Entry::getKey).findFirst().orElse(-1);
          if(ownedSeat>=0)session.leave(ownedSeat,request.requestId());
        }
        default->throw new IllegalArgumentException("unsupported NN command: "+request.msgId());
      }
    } catch (RuntimeException failure) {
      if (diagnostic) System.err.printf("[NN] command failed roomId=%d playerId=%d operationId=%s stateVersion=%d action=%s reason=%s%n",
          request.roomId(), playerId, request.requestId(), session.stateVersion(), request.msgId(), failure.getMessage());
      throw failure;
    }
    if (diagnostic) System.out.printf("[NN] command accepted roomId=%d playerId=%d operationId=%s stateVersion=%d action=%s%n",
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
    var result=session.lastResult(); if(result==null||result.round()!=roundNo)throw new IllegalStateException("NN round is not settled");
    RoomScoreLedger ledger = roomScoreLedger;
    if (ledger != null) {
      Map<Integer, Long> players = session.players();
      Map<Integer, Long> carryBySeat = new NnScoreSettlementPort(ledger)
          .capture(session.roomId(), players).carryBySeat();
      Comparator<Integer> strongestFirst = Comparator
          .<Integer, NnHandEvaluator.Hand>comparing(result.hands()::get).reversed()
          .thenComparingInt(Integer::intValue);
      Comparator<Integer> weakestFirst = Comparator
          .<Integer, NnHandEvaluator.Hand>comparing(result.hands()::get)
          .thenComparing(Comparator.reverseOrder());
      List<Integer> winnerOrder = result.scoreDelta().entrySet().stream()
          .filter(entry -> entry.getKey() != result.bankerSeat() && entry.getValue() > 0)
          .map(Map.Entry::getKey).sorted(strongestFirst).toList();
      List<Integer> loserOrder = result.scoreDelta().entrySet().stream()
          .filter(entry -> entry.getKey() != result.bankerSeat() && entry.getValue() < 0)
          .map(Map.Entry::getKey).sorted(weakestFirst).toList();
      Map<Integer, Long> theoreticalAmounts = new LinkedHashMap<>();
      result.scoreDelta().forEach((seat, score) -> {
        if (seat != result.bankerSeat()) theoreticalAmounts.put(seat, Math.abs(score));
      });
      Map<Integer, Long> allocated = NnSettlementAllocator.allocate(result.bankerSeat(), winnerOrder,
          loserOrder, theoreticalAmounts, carryBySeat);
      Map<Long,Long> delta=new LinkedHashMap<>();
      allocated.forEach((seat,score)->delta.put(players.get(seat),score));
      // Provider 只产出携带分封顶 delta；账户锁定与最终落账由共享账本调用方负责。
      return new SettlementPayload(session.roomId(),roundNo,playVersion,delta);
    }
    // 共享账本未接线时保留历史直发路径；该 payload 未执行携带分封顶。
    System.err.printf("[NN] settlement roomId=%d round=%d uses uncapped delta because RoomScoreLedger is not attached%n",
        session.roomId(), roundNo);
    Map<Long,Long> delta=new LinkedHashMap<>();
    result.scoreDelta().forEach((seat,score)->delta.put(session.players().get(seat),score));
    return new SettlementPayload(session.roomId(),roundNo,playVersion,delta);
  }
  private static long player(String id){try{return Long.parseLong(id);}catch(NumberFormatException e){throw new IllegalArgumentException("NN numeric player id required",e);}}
  private void resolvePlayerName(ParticipantProfileSource source,int seat,long playerId){
    String nickname=source.nickname(playerId);session.setPlayerName(seat,nickname==null||nickname.isBlank()?"玩家"+playerId:nickname);
  }
  private void requireSeatOwner(int seat, long playerId) {
    if (!Long.valueOf(playerId).equals(session.players().get(seat))) {
      throw new IllegalArgumentException("NN authenticated player does not own requested seat");
    }
  }
}
