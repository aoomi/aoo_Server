package com.aoo.bcg.poker;

import com.aoo.bcg.common.dissolve.DissolveDecision;
import com.aoo.bcg.common.dissolve.DissolveVoteState;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.gamespi.time.*;
import java.time.Duration;
import java.util.*;

/** 跑得快唯一权威房间状态机。生命周期、座位、轮转、牌权、超时、重连、回放和解散均在此闭环； 地方牌型与算分只能经 PdkVariantPolicy 注入，避免形成地区状态机或反向依赖。 */
public final class PokerAuthoritativeSession
    implements AuthoritativeGameSession,
        RoomLifecycleAuthority,
        ParticipantPresenceAuthority,
        RoomAdmissionAuthority {
  private static final Map<String, Boolean> DEFAULT_UI_CAPABILITIES =
      Map.of("addDouble", false, "robDoor", false, "openCard", false);
  private static final Duration DISSOLVE_VOTE_TIMEOUT = Duration.ofSeconds(120);
  private final long roomId, seed;
  private long ownerId;
  private final int seatLimit, roundLimit;
  private Integer previousWinnerSeat;
  private final PdkVariantPolicy policy;
  private final PaoDeKuaiFamily family;
  private final Map<Integer, Long> players = new LinkedHashMap<>(),
      observers = new LinkedHashMap<>();
  private final Set<Integer> readySeats = new LinkedHashSet<>(),
      continueSeats = new LinkedHashSet<>(),
      competeRespondedSeats = new LinkedHashSet<>(),
      hostingSeats = new LinkedHashSet<>();
  private final Map<Integer, Integer> plays = new LinkedHashMap<>(),
      bombs = new LinkedHashMap<>(),
      playedCardCounts = new LinkedHashMap<>(),
      initialPatternCounts = new LinkedHashMap<>(),
      missedOperations = new LinkedHashMap<>();
  private final Map<Integer, Long> totalScores = new LinkedHashMap<>();
  private final Map<Integer, List<Integer>> playedCardsBySeat = new LinkedHashMap<>();
  private final Map<Long, Long> offlineSinceEpochMillis = new LinkedHashMap<>();
  private final Map<Long, RoomAdmissionAuthority.Admission> admissions = new LinkedHashMap<>();
  private final List<Object> events = new ArrayList<>();
  private final List<CardCombination> playedCards = new ArrayList<>();
  private final List<Map<String, Object>> playHistory = new ArrayList<>();
  private Integer activeRequiredFirstCard;
  private int initialLeadSeat = -1,
      bankerSeat = -1,
      directWinnerSeat = -1,
      jinHuaWinnerSeat = -1,
      competeDealerSeat = -1,
      competeCursor = -1,
      roundNo;
  private boolean roundScored, competeDealerPhase;
  private PokerTurnState state;
  private final AuthoritativeTimeSource time = AuthoritativeTimeSource.systemUtc();
  private OperationDeadline deadline = OperationDeadline.none();
  private final OperationDeadlineArbiter arbiter = new OperationDeadlineArbiter();
  private final Map<Integer, Integer> winCounts = new LinkedHashMap<>(),
      loseCounts = new LinkedHashMap<>();
  private long stateVersion;
  private long trickId;
  private DissolveVoteState dissolveVote;
  private boolean dissolved;
  private String dissolveReason = "";
  private final Map<Integer, Map<String, Object>> lastActions = new LinkedHashMap<>();

  public PokerAuthoritativeSession(
      long roomId, long ownerId, int seatLimit, long seed, PaoDeKuaiFamily family) {
    this(
        roomId,
        ownerId,
        seatLimit,
        seed,
        PdkVariantPolicy.standard(family),
        null,
        Integer.MAX_VALUE);
  }

  public PokerAuthoritativeSession(
      long roomId, long ownerId, int seatLimit, long seed, PaoDeKuaiFamily family, int roundLimit) {
    this(roomId, ownerId, seatLimit, seed, PdkVariantPolicy.standard(family), null, roundLimit);
  }

  public PokerAuthoritativeSession(
      long roomId, long ownerId, int seatLimit, long seed, PdkVariantPolicy policy) {
    this(roomId, ownerId, seatLimit, seed, policy, null, Integer.MAX_VALUE);
  }

  public PokerAuthoritativeSession(
      long roomId,
      long ownerId,
      int seatLimit,
      long seed,
      PdkVariantPolicy policy,
      int roundLimit) {
    this(roomId, ownerId, seatLimit, seed, policy, null, roundLimit);
  }

  private PokerAuthoritativeSession(
      long roomId,
      long ownerId,
      int seatLimit,
      long seed,
      PdkVariantPolicy policy,
      Integer previousWinnerSeat,
      int roundLimit) {
    if (roomId <= 0 || ownerId <= 0 || seatLimit < 2 || seatLimit > 4 || roundLimit <= 0)
      throw new IllegalArgumentException("invalid poker session");
    this.roomId = roomId;
    this.ownerId = ownerId;
    this.seatLimit = seatLimit;
    this.seed = seed;
    this.policy = Objects.requireNonNull(policy);
    this.family = policy.family();
    this.previousWinnerSeat = previousWinnerSeat;
    this.roundLimit = roundLimit;
    players.put(0, ownerId);
    plays.put(0, 0);
    bombs.put(0, 0);
    playedCardCounts.put(0, 0);
    initialPatternCounts.put(0, 0);
    missedOperations.put(0, 0);
    totalScores.put(0, 0L);
    ensureStatistics(0);
  }

  private void ensureStatistics(int seat) {
    winCounts.putIfAbsent(seat, 0);
    loseCounts.putIfAbsent(seat, 0);
  }

  public static PokerAuthoritativeSession restore(Map<String, Object> s, PaoDeKuaiFamily family) {
    return restore(s, PdkVariantPolicy.standard(family));
  }

  public static PokerAuthoritativeSession restore(Map<String, Object> s, PdkVariantPolicy policy) {
    PaoDeKuaiFamily family = policy.family();
    if (!family.profile().version().equals(String.valueOf(s.get("ruleVersion")))
        || !family.ruleSnapshotKey().equals(String.valueOf(s.get("ruleSnapshotKey"))))
      throw new IllegalStateException("rule snapshot mismatch");
    if (s.containsKey("variantPolicyId")
        && !policy.policyId().equals(String.valueOf(s.get("variantPolicyId"))))
      throw new IllegalStateException("variant policy mismatch");
    Integer previous =
        s.get("previousWinnerSeat") instanceof Number n && n.intValue() >= 0 ? n.intValue() : null;
    int roundLimit = s.get("roundLimit") instanceof Number n ? n.intValue() : Integer.MAX_VALUE;
    var x =
        new PokerAuthoritativeSession(
            lng(s.get("roomId")),
            lng(s.get("ownerId")),
            num(s.get("seatLimit")),
            lng(s.get("seed")),
            policy,
            previous,
            roundLimit);
    x.stateVersion = s.get("stateVersion") instanceof Number n ? n.longValue() : 0;
    x.trickId = s.get("trickId") instanceof Number n ? n.longValue() : 0;
    x.initialLeadSeat = s.get("initialLeadSeat") instanceof Number n ? n.intValue() : -1;
    x.bankerSeat = s.get("bankerSeat") instanceof Number n ? n.intValue() : x.initialLeadSeat;
    x.directWinnerSeat = s.get("directWinnerSeat") instanceof Number n ? n.intValue() : -1;
    x.jinHuaWinnerSeat = s.get("jinHuaWinnerSeat") instanceof Number n ? n.intValue() : -1;
    x.competeDealerSeat = s.get("competeDealerSeat") instanceof Number n ? n.intValue() : -1;
    x.competeCursor = s.get("competeCursor") instanceof Number n ? n.intValue() : -1;
    x.activeRequiredFirstCard =
        s.get("activeRequiredFirstCard") instanceof Number n ? n.intValue() : null;
    x.competeDealerPhase = bool(s.getOrDefault("competeDealerPhase", false));
    x.players.clear();
    x.observers.clear();
    x.plays.clear();
    x.bombs.clear();
    x.playedCardCounts.clear();
    x.initialPatternCounts.clear();
    x.missedOperations.clear();
    x.totalScores.clear();
    x.winCounts.clear();
    x.loseCounts.clear();
    x.players.putAll(longMap(s.get("players")));
    x.observers.putAll(longMap(s.get("observers")));
    if (s.get("readySeats") instanceof Collection<?> c) c.forEach(v -> x.readySeats.add(num(v)));
    if (s.get("continueSeats") instanceof Collection<?> c)
      c.forEach(v -> x.continueSeats.add(num(v)));
    if (s.get("competeRespondedSeats") instanceof Collection<?> c)
      c.forEach(v -> x.competeRespondedSeats.add(num(v)));
    if (s.get("hostingSeats") instanceof Collection<?> c)
      c.forEach(v -> x.hostingSeats.add(num(v)));
    x.plays.putAll(intMap(s.get("plays")));
    x.bombs.putAll(intMap(s.get("bombs")));
    x.playedCardCounts.putAll(intMap(s.get("playedCardCounts")));
    x.initialPatternCounts.putAll(intMap(s.get("initialPatternCounts")));
    x.missedOperations.putAll(intMap(s.get("missedOperations")));
    x.totalScores.putAll(longMap(s.get("totalScores")));
    x.winCounts.putAll(intMap(s.get("winCounts")));
    x.loseCounts.putAll(intMap(s.get("loseCounts")));
    x.offlineSinceEpochMillis.putAll(longLongMap(s.get("offlineSinceEpochMillis")));
    x.admissions.putAll(admissionMap(s.get("admissions")));
    x.players
        .keySet()
        .forEach(
            seat -> {
              x.totalScores.putIfAbsent(seat, 0L);
              x.playedCardCounts.putIfAbsent(seat, 0);
              x.initialPatternCounts.putIfAbsent(seat, 0);
              x.missedOperations.putIfAbsent(seat, 0);
              x.ensureStatistics(seat);
            });
    x.playedCards.addAll(cardCombinations(s.get("playedCards")));
    x.playHistory.addAll(playHistory(s.get("playHistory")));
    x.lastActions.putAll(lastActions(s.get("lastActions")));
    if (s.get("playedCardsBySeat") instanceof Map<?, ?> m)
      m.forEach(
          (seat, cards) ->
              x.playedCardsBySeat.put(
                  Integer.parseInt(String.valueOf(seat)), new ArrayList<>(numbers(cards))));
    if (s.get("state") instanceof Map<?, ?> m) x.state = restoreState(m);
    else if (s.get("state") instanceof PokerTurnState p) x.state = p;
    x.roundNo = s.get("roundNo") instanceof Number n ? n.intValue() : (x.state == null ? 0 : 1);
    x.roundScored = bool(s.getOrDefault("roundScored", x.state != null && x.state.finished()));
    x.deadline = OperationDeadline.from(s.get("operationDeadline"));
    x.dissolved = bool(s.getOrDefault("dissolved", false));
    x.dissolveReason = String.valueOf(s.getOrDefault("dissolveReason", ""));
    if (s.get("dissolveVote") instanceof Map<?, ?> m)
      x.dissolveVote = DissolveVoteState.from(stringMap(m));
    return com.aoo.bcg.gamespi.fsm.RestoredAuthorityValidator.requireConsistent(x);
  }

  @Override
  public synchronized GameCommandResult execute(GameCommandRequest r) {
    long player = player(r.authenticatedUserId());
    String op = operation(r);
    if (dissolved && !op.equals("state")) throw new IllegalStateException("room is dissolved");
    Map<String, Object> body;
    boolean changed = !op.equals("state") && !op.equals("hint");
    switch (op) {
      case "join" -> {
        Long seatPlayer = players.get(r.seatId()), observer = observers.get(r.seatId());
        Integer playerSeat = seatOfNullable(player), observerSeat = observerSeatOf(player);
        PdkAdvancedRules.RoomGovernance governance =
            family.rules().config().advancedRules().governance();
        if (governance.entryMode() == PdkAdvancedRules.EntryMode.OBSERVER) {
          if (Objects.equals(observer, player) && Objects.equals(observerSeat, r.seatId()))
            changed = false;
          else {
            if (state != null
                || r.seatId() < 0
                || r.seatId() >= seatLimit
                || seatPlayer != null
                || observer != null
                || playerSeat != null
                || observerSeat != null)
              throw new IllegalStateException("observer reservation occupied");
            observers.put(r.seatId(), player);
          }
          body = viewFor(player);
        } else if (Objects.equals(seatPlayer, player) && Objects.equals(playerSeat, r.seatId())) {
          changed = false;
          body = viewFor(player);
        } else {
          if (state != null
              || r.seatId() < 0
              || r.seatId() >= seatLimit
              || seatPlayer != null
              || observer != null
              || playerSeat != null
              || observerSeat != null) throw new IllegalStateException("seat/player occupied");
          players.put(r.seatId(), player);
          plays.put(r.seatId(), 0);
          bombs.put(r.seatId(), 0);
          playedCardCounts.put(r.seatId(), 0);
          initialPatternCounts.put(r.seatId(), 0);
          missedOperations.put(r.seatId(), 0);
          totalScores.put(r.seatId(), 0L);
          ensureStatistics(r.seatId());
          body = viewFor(player);
        }
      }
      case "take_seat" -> {
        if (state != null) throw new IllegalStateException("cannot take seat after round start");
        if (!Long.valueOf(player).equals(observers.get(r.seatId()))
            || players.containsKey(r.seatId()))
          throw new SecurityException("observer reservation not owned");
        enforceAdmission(player, admissions.get(player));
        observers.remove(r.seatId());
        players.put(r.seatId(), player);
        plays.put(r.seatId(), 0);
        bombs.put(r.seatId(), 0);
        playedCardCounts.put(r.seatId(), 0);
        initialPatternCounts.put(r.seatId(), 0);
        missedOperations.put(r.seatId(), 0);
        totalScores.put(r.seatId(), 0L);
        ensureStatistics(r.seatId());
        body = viewFor(player);
      }
      case "ready" -> {
        if (state != null) {
          if (!Long.valueOf(player).equals(players.get(r.seatId())))
            throw new SecurityException("seat not owned");
          changed = false;
          body = viewFor(player);
        } else {
          if (!Long.valueOf(player).equals(players.get(r.seatId())))
            throw new SecurityException("seat not owned");
          boolean readyChanged = readySeats.add(r.seatId());
          changed = autoStartIfAllReady() || readyChanged;
          body = viewFor(player);
        }
      }
      case "unready" -> {
        if (state != null) throw new IllegalStateException("round already started");
        if (!Long.valueOf(player).equals(players.get(r.seatId())))
          throw new SecurityException("seat not owned");
        changed = readySeats.remove(r.seatId());
        body = viewFor(player);
      }
      case "start" ->
          throw new IllegalStateException("players must click ready; manual start is disabled");
      case "trusteeship" -> {
        own(player, r.seatId());
        requirePlaying();
        boolean enabled = bool(commandBody(r).getOrDefault("trusteeship", true));
        if (enabled) hostingSeats.add(r.seatId());
        else resetHosting(r.seatId());
        body = viewFor(player);
      }
      case "state" -> body = viewFor(player);
      case "hint" -> {
        own(player, r.seatId());
        requirePlaying();
        if (state.currentSeat() != r.seatId()) throw new IllegalStateException("not current turn");
        var hints = family.rules().hints(state.hands().get(r.seatId()), state.previous(), context(r));
        body = Map.of(
            "hints", hints,
            "roomId", roomId,
            "stateVersion", stateVersion,
            "operationId", deadline.operationId(),
            "turnSeat", state.currentSeat(),
            "canPass", state.previous() != null && hints.isEmpty());
      }
      case "pass" -> {
        own(player, r.seatId());
        requirePlaying();
        PaoDeKuaiContext c = context(r);
        if (family.profile().mustBeatWhenPossible()
            && !family.rules().hints(state.hands().get(r.seatId()), state.previous(), c).isEmpty())
          throw new IllegalStateException("must beat when possible");
        state = new PokerCoreEngine<Void>().pass(state, r.seatId());
        if (state.previous() == null) {
          lastActions.clear();
          trickId = Math.addExact(trickId, 1);
        }
        else lastActions.put(r.seatId(), lastAction(r.seatId(), "pass", List.of(), "PASS", r.requestId()));
        resetHosting(r.seatId());
        body = viewFor(player);
      }
      case "compete_dealer", "rob_dealer" -> {
        own(player, r.seatId());
        boolean compete = bool(commandBody(r).getOrDefault("compete", true));
        competeDealer(r.seatId(), compete);
        resetHosting(r.seatId());
        body = viewFor(player);
      }
      case "play", "play_cards", "play_card" -> {
        own(player, r.seatId());
        requirePlaying();
        List<Integer> cards = numbers(commandBody(r).get("cards"));
        PaoDeKuaiContext c = context(r);
        family.rules().validatePlay(cards, c);
        CardCombination combination = family.rules().recognize(cards, c);
        policy.validatePattern(r, combination, c);
        CardCombination previous = state.previous();
        int previousSeat = state.previousSeat();
        state =
            new PokerCoreEngine<PaoDeKuaiContext>()
                .play(state, r.seatId(), cards, family.rules(), c);
        recordPlay(r.seatId(), combination);
        lastActions.put(
            r.seatId(),
            lastAction(r.seatId(), "play", cards, combination.type(), r.requestId()));
        playedCardCounts.merge(r.seatId(), cards.size(), Integer::sum);
        if (family.rules().isBomb(combination)) {
          bombs.merge(r.seatId(), 1, Integer::sum);
          if (family.rules().config().advancedRules().bombScore().mode()
                  == PdkAdvancedRules.BombMode.FIXED_POINTS
              && previous != null
              && family.rules().isBomb(previous))
            bombs.computeIfPresent(previousSeat, (seat, count) -> Math.max(0, count - 1));
        }
        resetHosting(r.seatId());
        if (state.finished()) completeRound();
        body = viewFor(player);
      }
      case "continue" -> {
        own(player, r.seatId());
        if (!roundFinished()) throw new IllegalStateException("round not finished");
        if (roundNo >= roundLimit) throw new IllegalStateException("match already finished");
        continueSeats.add(r.seatId());
        if (continueSeats.containsAll(players.keySet())) {
          previousWinnerSeat = winnerSeat();
          state = null;
          readySeats.clear();
          readySeats.addAll(players.keySet());
          start();
        }
        body = viewFor(player);
      }
      case "rematch" -> {
        own(player, r.seatId());
        if (!roundFinished() || roundNo < roundLimit)
          throw new IllegalStateException("match not finished");
        continueSeats.add(r.seatId());
        if (continueSeats.containsAll(players.keySet())) {
          state = null;
          roundNo = 0;
          roundScored = false;
          previousWinnerSeat = null;
          totalScores.clear();
          winCounts.clear();
          loseCounts.clear();
          players.keySet().forEach(this::ensureStatistics);
          plays.clear();
          bombs.clear();
          playedCardCounts.clear();
          initialPatternCounts.clear();
          readySeats.clear();
          readySeats.addAll(players.keySet());
          start();
        }
        body = viewFor(player);
      }
      case "leave" -> {
        leave(player, r.seatId());
        body = viewFor(player);
      }
      case "dissolve" -> {
        requestDissolve(player);
        body = viewFor(player);
      }
      case "dissolve_agree" -> {
        voteDissolve(player, true);
        body = viewFor(player);
      }
      case "dissolve_refuse" -> {
        voteDissolve(player, false);
        body = viewFor(player);
      }
      case "text_chat" -> {
        seatOf(player);
        if (!family.rules().config().advancedRules().governance().textChatEnabled())
          throw new SecurityException("text chat disabled by room rule");
        body = Map.of("accepted", true);
      }
      case "preset_interaction", "voice_interaction" -> {
        seatOf(player);
        if (!family.rules().config().advancedRules().governance().interactionEnabled())
          throw new SecurityException("interaction disabled by room rule");
        body = Map.of("accepted", true);
      }
      default -> throw new IllegalArgumentException("unsupported poker command: " + r.msgId());
    }
    if (changed) {
      stateVersion = Math.addExact(stateVersion, 1);
      if (state != null && !roundFinished()) openDeadline(op);
      if (roundFinished()) deadline = OperationDeadline.none();
      body = viewFor(player);
      events.add(Map.of("version", stateVersion, "after", authoritativeState()));
    } else if (roundFinished()) deadline = OperationDeadline.none();
    return new GameCommandResult(r.msgId().replace("_req", "_resp"), r.requestId(), body);
  }

  @Override
  public synchronized long stateVersion() {
    return stateVersion;
  }

  private boolean autoStartIfAllReady() {
    if (state != null || players.size() != seatLimit || !readySeats.containsAll(players.keySet()))
      return false;
    start();
    return true;
  }

  private void start() {
    if (state != null) throw new IllegalStateException("already started");
    PokerRuleProfile p = family.profile();
    int count = family.rules().cardsPerPlayer(players.size());
    List<Integer> deck = new ArrayList<>(p.deck());
    new SeededGameRandomSource(seed + roundNo).shuffle(deck);
    Map<Integer, List<Integer>> hands = new LinkedHashMap<>();
    playedCards.clear();
    playedCardsBySeat.clear();
    playHistory.clear();
    lastActions.clear();
    trickId = Math.addExact(trickId, 1);
    continueSeats.clear();
    competeRespondedSeats.clear();
    directWinnerSeat = -1;
    jinHuaWinnerSeat = -1;
    competeDealerSeat = -1;
    competeCursor = -1;
    competeDealerPhase = false;
    for (int seat : players.keySet().stream().sorted().toList()) {
      hands.put(seat, new ArrayList<>(deck.subList(0, count)));
      deck.subList(0, count).clear();
      plays.put(seat, 0);
      bombs.put(seat, 0);
      playedCardCounts.put(seat, 0);
      initialPatternCounts.put(
          seat, PdkInitialHandEvaluator.patterns(hands.get(seat), family.rules().config()).size());
    }
    int nextRound = roundNo + 1;
    bankerSeat = resolveBanker(p, hands, nextRound);
    initialLeadSeat = bankerSeat;
    activeRequiredFirstCard = resolveRequiredFirstCard(hands, nextRound);
    roundNo = Math.addExact(roundNo, 1);
    roundScored = false;
    continueSeats.clear();
    state = new PokerTurnState(hands, bankerSeat, null, -1, Set.of(), false, -1);
    PdkAdvancedRules advanced = family.rules().config().advancedRules();
    if (advanced.jinHuaScoreUnit() > 0)
      jinHuaWinnerSeat =
          hands.keySet().stream()
              .max(
                  (left, right) ->
                      PdkInitialHandEvaluator.bestJinHua(hands.get(left))
                          .compareTo(PdkInitialHandEvaluator.bestJinHua(hands.get(right))))
              .orElse(-1);
    directWinnerSeat =
        hands.keySet().stream()
            .sorted()
            .filter(seat -> PdkInitialHandEvaluator.directWin(hands.get(seat), advanced))
            .findFirst()
            .orElse(-1);
    if (directWinnerSeat >= 0) {
      completeRound();
      return;
    }
    PdkAdvancedRules.DealerRule dealer = advanced.dealerRule();
    if (dealer.enabled() && !(dealer.skipFirstRound() && roundNo == 1)) {
      competeDealerPhase = true;
      competeCursor = dealer.startAfterBanker() ? nextSeat(bankerSeat) : bankerSeat;
    }
  }

  private int resolveBanker(
      PokerRuleProfile profile, Map<Integer, List<Integer>> hands, int nextRound) {
    PdkAdvancedRules advanced = family.rules().config().advancedRules();
    if (advanced.bankerSelectionCard() != null
        && (nextRound == 1 || advanced.selectBankerEveryRound()))
      return hands.entrySet().stream()
          .filter(entry -> entry.getValue().contains(advanced.bankerSelectionCard()))
          .map(Map.Entry::getKey)
          .findFirst()
          .orElseGet(() -> minimumCardHolder(hands));
    if (family.rules().config().requiredFirstCard() != null
        && nextRound <= advanced.requiredFirstCardRounds())
      return hands.entrySet().stream()
          .filter(entry -> entry.getValue().contains(family.rules().config().requiredFirstCard()))
          .map(Map.Entry::getKey)
          .findFirst()
          .orElseGet(() -> minimumCardHolder(hands));
    if (previousWinnerSeat != null && hands.containsKey(previousWinnerSeat))
      return previousWinnerSeat;
    if (profile.firstLead() == PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER)
      return minimumCardHolder(hands);
    return PokerFirstLeadResolver.resolve(
        profile, hands, seatOf(ownerId), previousWinnerSeat, seed + roundNo);
  }

  private Integer resolveRequiredFirstCard(Map<Integer, List<Integer>> hands, int nextRound) {
    Integer required = family.rules().config().requiredFirstCard();
    if (required == null
        || nextRound > family.rules().config().advancedRules().requiredFirstCardRounds())
      return null;
    List<Integer> bankerHand = hands.get(bankerSeat);
    if (bankerHand.contains(required)) return required;
    return bankerHand.stream()
        .min(
            Comparator.comparingInt((Integer card) -> StandardPokerRuleSet.rank(card))
                .thenComparingInt(Integer::intValue))
        .orElseThrow();
  }

  private int minimumCardHolder(Map<Integer, List<Integer>> hands) {
    return hands.entrySet().stream()
        .min(
            Comparator.comparingInt(
                entry ->
                    entry.getValue().stream()
                        .min(
                            Comparator.comparingInt(
                                    (Integer card) -> StandardPokerRuleSet.rank(card))
                                .thenComparingInt(Integer::intValue))
                        .orElseThrow()))
        .map(Map.Entry::getKey)
        .orElseThrow();
  }

  private void competeDealer(int seat, boolean compete) {
    if (!competeDealerPhase || seat != competeCursor)
      throw new IllegalStateException("not compete-dealer turn");
    PdkAdvancedRules.DealerRule rule = family.rules().config().advancedRules().dealerRule();
    competeRespondedSeats.add(seat);
    if (compete) {
      competeDealerSeat = seat;
      activeRequiredFirstCard = null;
      if (!rule.startAfterBanker() || seat == bankerSeat) {
        finishCompeteDealer(seat);
        return;
      }
    }
    if (seat == bankerSeat && competeDealerSeat >= 0) {
      finishCompeteDealer(competeDealerSeat);
      return;
    }
    if (competeRespondedSeats.containsAll(players.keySet())) {
      finishCompeteDealer(competeDealerSeat >= 0 ? competeDealerSeat : bankerSeat);
      return;
    }
    competeCursor = nextSeat(competeCursor);
  }

  private void finishCompeteDealer(int seat) {
    competeDealerSeat = seat;
    initialLeadSeat = seat;
    competeDealerPhase = false;
    competeCursor = -1;
    state = new PokerTurnState(state.hands(), seat, null, -1, Set.of(), false, -1);
  }

  private int nextSeat(int seat) {
    List<Integer> ordered = players.keySet().stream().sorted().toList();
    return ordered.get((ordered.indexOf(seat) + 1) % ordered.size());
  }

  private void completeRound() {
    if (roundScored) return;
    SettlementPayload settlement = settlementFor(roundNo);
    settlement
        .scoreDelta()
        .forEach((player, score) -> totalScores.merge(seatOf(player), score, Long::sum));
    int winner = winnerSeat();
    players
        .keySet()
        .forEach(
            seat -> {
              ensureStatistics(seat);
              if (seat == winner) winCounts.merge(seat, 1, Integer::sum);
              else loseCounts.merge(seat, 1, Integer::sum);
            });
    roundScored = true;
  }

  private PaoDeKuaiContext context(GameCommandRequest r) {
    CommandPayload command = commandBody(r);
    List<Integer> seats = state.hands().keySet().stream().sorted().toList();
    int next = seats.get((seats.indexOf(r.seatId()) + 1) % seats.size()), attempts = 0;
    while (state.passed().contains(next) && attempts++ < seats.size())
      next = seats.get((seats.indexOf(next) + 1) % seats.size());
    if (attempts >= seats.size() || state.passed().contains(next))
      throw new IllegalStateException("no active next seat");
    int count = state.hands().get(next).size();
    if (command.containsKey("nextPlayerCardCount")) {
      Object supplied = command.get("nextPlayerCardCount");
      if (!(supplied instanceof Number n) || n.intValue() != count)
        throw new IllegalArgumentException("client card count conflicts with authority");
    }
    boolean first = plays.values().stream().mapToInt(Integer::intValue).sum() == 0;
    return new PaoDeKuaiContext(
        first, count, state.hands().get(r.seatId()), activeRequiredFirstCard, true);
  }

  @Override
  public synchronized Map<String, Object> viewFor(long viewer) {
    Integer own = seatOfNullable(viewer);
    Map<Long, Long> roundScores = roundFinished() ? settlementFor(roundNo).scoreDelta() : Map.of();
    Map<Integer, Object> seats = new LinkedHashMap<>();
    players.forEach(
        (seat, id) -> {
          List<Integer> cards = state == null ? List.of() : state.hands().get(seat);
          seats.put(seat, seatView(seat, id, cards, own, roundScores));
        });
    boolean matchFinished = roundFinished() && roundNo >= roundLimit;
    Map<String, Object> o = new LinkedHashMap<>();
    o.put("roomId", roomId);
    o.put("ownerId", ownerId);
    o.put("family", family.familyCode());
    o.put("playerCount", seatLimit);
    o.put("seatLimit", seatLimit);
    o.put("roundNo", roundNo);
    o.put("roundLimit", roundLimit);
    o.put("playVersion", family.profile().version());
    o.put("stateVersion", stateVersion);
    o.put("capabilities", DEFAULT_UI_CAPABILITIES);
    o.put(
        "ruleOptions", PdkPublishedRuleOptions.snapshot(family.rules().config(), family.profile()));
    o.put(
        "phase",
        state == null
            ? "WAITING"
            : directWinnerSeat >= 0
                ? "DIRECT_WIN"
                : competeDealerPhase
                    ? "COMPETE_DEALER"
                    : state.finished() ? "FINISHED" : "PLAYING");
    o.put("started", state != null);
    o.put("currentSeat", currentSeat());
    o.put("currentPlayerId", players.getOrDefault(currentSeat(), 0L));
    o.put("finished", roundFinished());
    o.put("matchFinished", matchFinished);
    o.put("canContinue", roundFinished() && !matchFinished);
    o.put("winnerSeat", winnerSeat());
    o.put("bankerSeat", bankerSeat);
    o.put("competeDealerSeat", competeDealerSeat);
    o.put("seats", Map.copyOf(seats));
    o.put("observers", Map.copyOf(observers));
    PdkAdvancedRules.RoomGovernance governance =
        family.rules().config().advancedRules().governance();
    o.put("settlementPresentation", governance.settlementPresentation().name());
    o.put("distanceWarningEnabled", governance.distanceWarningEnabled());
    o.put("textChatEnabled", governance.textChatEnabled());
    o.put("interactionEnabled", governance.interactionEnabled());
    List<CardCombination> visiblePlays =
        family.rules().config().playedCardVisibility()
                == PaoDeKuaiConfig.PlayedCardVisibility.ALL_IN_ORDER
            ? List.copyOf(playedCards)
            : (playedCards.isEmpty()
                ? List.of()
                : List.of(playedCards.get(playedCards.size() - 1)));
    o.put("playedCards", visiblePlays);
    o.put("lastActions", List.copyOf(lastActions.values()));
    o.put("trickId", trickId);
    o.put("trickReset", state != null && state.previous() == null);
    if (state != null && state.previous() != null)
      o.put(
          "currentTrick",
          Map.of(
              "seat",
              state.previousSeat(),
              "type",
              state.previous().type(),
              "primaryRank",
              state.previous().primaryRank(),
              "cards",
              state.previous().cards()));
    else o.put("currentTrick", Map.of());
    o.put("operationDeadline", deadline.toMap());
    o.put("serverEpochMillis", time.epochMillis());
    o.put("dissolved", dissolved);
    o.put("dissolveReason", dissolveReason);
    if (dissolveVote != null) o.put("dissolveVote", dissolveVote.toMap());
    return Map.copyOf(o);
  }

  private Map<String, Object> seatView(
      int seat,
      long playerId,
      List<Integer> cards,
      Integer viewerSeat,
      Map<Long, Long> roundScores) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("playerId", playerId);
    view.put("name", "玩家" + playerId);
    view.put("headImageUrl", "");
    view.put("ready", readySeats.contains(seat));
    view.put("continued", continueSeats.contains(seat));
    view.put("hosting", hostingSeats.contains(seat));
    view.put("offline", offlineSinceEpochMillis.containsKey(playerId));
    view.put(
        "cards",
        seat == Objects.requireNonNullElse(viewerSeat, -1) || roundFinished()
            ? cards
            : Collections.nCopies(cards.size(), 0));
    view.put("remainingCards", roundFinished() ? List.copyOf(cards) : List.of());
    view.put(
        "playedCards",
        roundFinished() ? List.copyOf(playedCardsBySeat.getOrDefault(seat, List.of())) : List.of());
    view.put("cardCount", cards.size());
    view.put("roundScore", roundScores.getOrDefault(playerId, 0L));
    view.put("totalScore", totalScores.getOrDefault(seat, 0L));
    view.put("winCount", winCounts.getOrDefault(seat, 0));
    view.put("loseCount", loseCounts.getOrDefault(seat, 0));
    return Map.copyOf(view);
  }

  @Override
  public synchronized Map<String, Object> authoritativeState() {
    Map<String, Object> o = new LinkedHashMap<>();
    o.put("roomId", roomId);
    o.put("ownerId", ownerId);
    o.put("seatLimit", seatLimit);
    o.put("roundLimit", roundLimit);
    o.put("seed", seed);
    o.put("roundNo", roundNo);
    o.put("players", Map.copyOf(players));
    o.put("observers", Map.copyOf(observers));
    o.put("readySeats", List.copyOf(readySeats));
    o.put("continueSeats", List.copyOf(continueSeats));
    o.put("competeRespondedSeats", List.copyOf(competeRespondedSeats));
    o.put("hostingSeats", List.copyOf(hostingSeats));
    o.put("plays", Map.copyOf(plays));
    o.put("bombs", Map.copyOf(bombs));
    o.put("playedCardCounts", Map.copyOf(playedCardCounts));
    o.put(
        "playedCardsBySeat",
        playedCardsBySeat.entrySet().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, e -> List.copyOf(e.getValue()))));
    o.put("initialPatternCounts", Map.copyOf(initialPatternCounts));
    o.put("missedOperations", Map.copyOf(missedOperations));
    o.put("totalScores", Map.copyOf(totalScores));
    o.put("winCounts", Map.copyOf(winCounts));
    o.put("loseCounts", Map.copyOf(loseCounts));
    o.put("offlineSinceEpochMillis", Map.copyOf(offlineSinceEpochMillis));
    o.put("admissions", admissionSnapshot());
    o.put("roundScored", roundScored);
    o.put("ruleVersion", family.profile().version());
    o.put("ruleSnapshotKey", family.ruleSnapshotKey());
    o.put("variantPolicyId", policy.policyId());
    o.put(
        "pdkRuleOptions",
        PdkPublishedRuleOptions.snapshot(family.rules().config(), family.profile()));
    o.put("playedCards", List.copyOf(playedCards));
    o.put("lastActions", new LinkedHashMap<>(lastActions));
    o.put("trickId", trickId);
    o.put("uiCapabilities", DEFAULT_UI_CAPABILITIES);
    o.put("previousWinnerSeat", previousWinnerSeat == null ? -1 : previousWinnerSeat);
    o.put("initialLeadSeat", initialLeadSeat);
    if (activeRequiredFirstCard != null) o.put("activeRequiredFirstCard", activeRequiredFirstCard);
    o.put("bankerSeat", bankerSeat);
    o.put("directWinnerSeat", directWinnerSeat);
    o.put("jinHuaWinnerSeat", jinHuaWinnerSeat);
    o.put("competeDealerSeat", competeDealerSeat);
    o.put("competeCursor", competeCursor);
    o.put("competeDealerPhase", competeDealerPhase);
    o.put("state", state == null ? "WAITING" : state);
    o.put("operationDeadline", deadline.toMap());
    o.put("stateVersion", stateVersion);
    o.put("dissolved", dissolved);
    o.put("dissolveReason", dissolveReason);
    if (dissolveVote != null) o.put("dissolveVote", dissolveVote.toMap());
    return Map.copyOf(o);
  }

  public synchronized List<Object> recordedEvents() {
    return List.copyOf(events);
  }

  public static StatePayload replay(StatePayload base, List<Object> orderedEvents) {
    Map<String, Object> current = new LinkedHashMap<>(base.asMap());
    long version = current.get("stateVersion") instanceof Number n ? n.longValue() : 0;
    for (Object raw : orderedEvents) {
      if (!(raw instanceof Map<?, ?> event))
        throw new IllegalArgumentException("invalid poker replay event");
      long next = lng(event.get("version"));
      if (next != version + 1) throw new IllegalArgumentException("poker replay version gap");
      Object after = event.get("after");
      if (!(after instanceof Map<?, ?> map))
        throw new IllegalArgumentException("invalid poker replay snapshot");
      Map<String, Object> replacement = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet())
        replacement.put(String.valueOf(entry.getKey()), entry.getValue());
      current = replacement;
      if (lng(current.get("stateVersion")) != next)
        throw new IllegalArgumentException("poker replay payload version mismatch");
      version = next;
    }
    return StatePayload.copyOf(current);
  }

  public synchronized PokerSettlementCalculator.Detail settlementDetail() {
    if (state == null || !state.finished()) throw new IllegalStateException("round not finished");
    Map<Integer, PokerSettlementCalculator.SeatInput> in = new LinkedHashMap<>();
    players.forEach(
        (seat, id) ->
            in.put(
                seat,
                new PokerSettlementCalculator.SeatInput(
                    id,
                    state.hands().get(seat).size(),
                    plays.getOrDefault(seat, 0),
                    bombs.getOrDefault(seat, 0))));
    PokerRuleProfile p = family.profile();
    return new PokerSettlementCalculator()
        .calculate(
            p.version(),
            state.winnerSeat(),
            bankerSeat >= 0 ? bankerSeat : initialLeadSeat,
            in,
            p.bombUnit(),
            p.multiplierCap());
  }

  @Override
  public synchronized SettlementPayload settlement(int round, String version) {
    if (!family.profile().version().equals(version))
      throw new IllegalArgumentException("settlement version differs from rule snapshot");
    return settlementFor(round);
  }

  private SettlementPayload settlementFor(int round) {
    if (!roundFinished()) throw new IllegalStateException("round not finished");
    return policy
        .scoringPolicy()
        .settle(
            new PdkSettlementContext(
                roomId,
                round,
                family.profile().version(),
                winnerSeat(),
                bankerSeat >= 0 ? bankerSeat : initialLeadSeat,
                players,
                state.hands(),
                plays,
                bombs,
                playedCardCounts,
                initialPatternCounts,
                jinHuaWinnerSeat,
                competeDealerSeat,
                family.profile(),
                family.rules().config()));
  }

  private boolean roundFinished() {
    return state != null && (state.finished() || directWinnerSeat >= 0);
  }

  private int winnerSeat() {
    return state == null ? -1 : directWinnerSeat >= 0 ? directWinnerSeat : state.winnerSeat();
  }

  private int currentSeat() {
    return state == null
        ? -1
        : competeDealerPhase ? competeCursor : roundFinished() ? winnerSeat() : state.currentSeat();
  }

  private void requirePlaying() {
    if (state == null || competeDealerPhase || roundFinished())
      throw new IllegalStateException("round is not accepting card operations");
  }

  private void resetHosting(int seat) {
    missedOperations.put(seat, 0);
    hostingSeats.remove(seat);
  }

  private void openDeadline(String operation) {
    deadline =
        OperationDeadline.open(
            roundNo + "-" + stateVersion + "-" + operation,
            currentSeat(),
            Duration.ofSeconds(family.rules().config().advancedRules().operationTimeoutSeconds()),
            time);
  }

  @Override
  public synchronized OperationDeadline operationDeadline() {
    return deadline;
  }

  @Override
  public OperationDeadlineArbiter deadlineArbiter() {
    return arbiter;
  }

  @Override
  public synchronized List<String> invariantViolations() {
    List<String> e = new ArrayList<>();
    if (roundLimit <= 0 || roundNo < 0 || roundNo > roundLimit) e.add("INVALID_ROUND_LIMIT");
    if (new HashSet<>(players.values()).size() != players.size()) e.add("DUPLICATE_PLAYER");
    if (!players.keySet().containsAll(hostingSeats)) e.add("INVALID_HOSTING_SEAT");
    if (state != null) {
      try {
        family
            .profile()
            .validatePlayerCount(players.size(), family.rules().config().cardsPerPlayer());
      } catch (RuntimeException bad) {
        e.add("INVALID_PLAYER_PROFILE");
      }
      if (!players.containsKey(initialLeadSeat) || !players.containsKey(bankerSeat))
        e.add("INVALID_INITIAL_LEAD");
      if (competeDealerPhase && !players.containsKey(competeCursor))
        e.add("INVALID_COMPETE_CURSOR");
      if (directWinnerSeat >= 0 && !players.containsKey(directWinnerSeat))
        e.add("INVALID_DIRECT_WINNER");
      if (state.previous() == null && (!state.passed().isEmpty() || state.previousSeat() != -1))
        e.add("INVALID_EMPTY_TRICK");
      if (state.previous() != null && !players.containsKey(state.previousSeat()))
        e.add("INVALID_PREVIOUS_SEAT");
      if (state.finished()
          && (!players.containsKey(state.winnerSeat())
              || state.currentSeat() != state.winnerSeat()
              || !state.hands().getOrDefault(state.winnerSeat(), List.of(1)).isEmpty()))
        e.add("INVALID_WINNER");
      if (!plays.keySet().equals(players.keySet())
          || !bombs.keySet().equals(players.keySet())
          || !playedCardCounts.keySet().equals(players.keySet())
          || !initialPatternCounts.keySet().equals(players.keySet())
          || !missedOperations.keySet().equals(players.keySet())
          || plays.values().stream().anyMatch(v -> v < 0)
          || bombs.values().stream().anyMatch(v -> v < 0)
          || playedCardCounts.values().stream().anyMatch(v -> v < 0)
          || initialPatternCounts.values().stream().anyMatch(v -> v < 0)
          || missedOperations.values().stream().anyMatch(v -> v < 0)) e.add("INVALID_STATS");
      if (!players.keySet().equals(state.hands().keySet())) e.add("SEAT_HAND_MISMATCH");
      if (state.hands().values().stream()
          .anyMatch(h -> h.size() > family.profile().maximumHandSize())) e.add("HAND_BUDGET");
      if (!players.keySet().containsAll(state.passed())
          || state.passed().contains(state.currentSeat())
          || state.passed().contains(state.previousSeat())) e.add("INVALID_PASSED");
      Set<Integer> seen = new HashSet<>();
      for (List<Integer> hand : state.hands().values())
        for (int card : hand)
          try {
            PokerCardCodec.validate(card);
            if (!family.profile().deck().contains(card)) e.add("CARD_OUTSIDE_PROFILE");
            if (!seen.add(card)) e.add("CARD_OWNERSHIP");
          } catch (IllegalArgumentException bad) {
            e.add("INVALID_CARD");
          }
    }
    return List.copyOf(e);
  }

  @Override
  public synchronized boolean tickLifecycle(java.time.Instant now) {
    if (dissolved) return false;
    if (dissolveVote != null) {
      DissolveDecision decision = dissolveVote.decision(now);
      if (decision == DissolveDecision.EXPIRED_APPROVED) {
        markDissolved("VOTE_TIMEOUT_APPROVED");
        recordLifecycleMutation();
        return true;
      }
      if (decision == DissolveDecision.EXPIRED_REJECTED) {
        dissolveReason = "VOTE_TIMEOUT_REJECTED";
        recordLifecycleMutation();
        return true;
      }
    }
    if (!deadline.open() || now.isBefore(deadline.deadline()) || roundFinished()) return false;
    OperationDeadline expired = deadline;
    AuthoritativeTimeSource fixed =
        new AuthoritativeTimeSource(java.time.Clock.fixed(now, java.time.ZoneOffset.UTC));
    arbiter.resolveTimeout(expired, fixed, this::autoOperate);
    stateVersion = Math.addExact(stateVersion, 1);
    if (roundFinished()) deadline = OperationDeadline.none();
    else openDeadline("timeout");
    events.add(Map.of("version", stateVersion, "after", authoritativeState()));
    return true;
  }

  private void autoOperate() {
    int seat = currentSeat();
    int misses = missedOperations.merge(seat, 1, Integer::sum);
    int threshold = family.rules().config().advancedRules().hostingMissThreshold();
    if (threshold > 0 && misses >= threshold) hostingSeats.add(seat);
    if (competeDealerPhase) {
      competeDealer(seat, false);
      return;
    }
    requirePlaying();
    PaoDeKuaiContext context = authoritativeContext(seat);
    List<CardCombination> legal = new ArrayList<>();
    for (CardCombination candidate :
        family.rules().hints(state.hands().get(seat), state.previous(), context))
      try {
        policy.validatePattern(
            new GameCommandRequest(
                "poker.auto_play_req",
                "timeout-" + stateVersion,
                stateVersion + 1,
                roomId,
                roundNo,
                family.profile().version(),
                String.valueOf(players.get(seat)),
                seat,
                Map.of("cards", candidate.cards())),
            candidate,
            context);
        legal.add(candidate);
      } catch (IllegalArgumentException ignored) {
        /* Variant policy rejected this generated candidate. */
      }
    if (legal.isEmpty()) {
      if (state.previous() == null) throw new IllegalStateException("no legal automatic lead");
      state = new PokerCoreEngine<Void>().pass(state, seat);
      if (state.previous() == null) {
        lastActions.clear();
        trickId = Math.addExact(trickId, 1);
      } else {
        lastActions.put(seat, lastAction(seat, "pass", List.of(), "PASS", "timeout-" + stateVersion));
      }
      return;
    }
    CardCombination combination = legal.getFirst(), previous = state.previous();
    int previousSeat = state.previousSeat();
    state =
        new PokerCoreEngine<PaoDeKuaiContext>()
            .play(state, seat, combination.cards(), family.rules(), context);
    recordPlay(seat, combination);
    lastActions.put(
        seat,
        lastAction(
            seat, "play", combination.cards(), combination.type(), "timeout-" + stateVersion));
    playedCardCounts.merge(seat, combination.cards().size(), Integer::sum);
    if (family.rules().isBomb(combination)) {
      bombs.merge(seat, 1, Integer::sum);
      if (family.rules().config().advancedRules().bombScore().mode()
              == PdkAdvancedRules.BombMode.FIXED_POINTS
          && previous != null
          && family.rules().isBomb(previous))
        bombs.computeIfPresent(previousSeat, (ignored, count) -> Math.max(0, count - 1));
    }
    if (state.finished()) completeRound();
  }

  private void recordPlay(int seat, CardCombination combination) {
    playedCards.add(combination);
    playedCardsBySeat
        .computeIfAbsent(seat, ignored -> new ArrayList<>())
        .addAll(combination.cards());
    plays.merge(seat, 1, Integer::sum);
    playHistory.add(
        Map.of(
            "playIndex",
            playHistory.size() + 1,
            "seat",
            seat,
            "type",
            combination.type(),
            "cards",
            List.copyOf(combination.cards())));
  }

  private PaoDeKuaiContext authoritativeContext(int seat) {
    List<Integer> ordered = state.hands().keySet().stream().sorted().toList();
    int next = ordered.get((ordered.indexOf(seat) + 1) % ordered.size()), attempts = 0;
    while (state.passed().contains(next) && attempts++ < ordered.size())
      next = ordered.get((ordered.indexOf(next) + 1) % ordered.size());
    if (attempts >= ordered.size() || state.passed().contains(next))
      throw new IllegalStateException("no active next seat");
    return new PaoDeKuaiContext(
        plays.values().stream().mapToInt(Integer::intValue).sum() == 0,
        state.hands().get(next).size(),
        state.hands().get(seat),
        activeRequiredFirstCard,
        true);
  }

  @Override
  public synchronized boolean isTerminal() {
    return dissolved;
  }

  @Override
  public synchronized String terminalReason() {
    return dissolveReason.isBlank() ? "ROOM_DISSOLVED" : dissolveReason;
  }

  private void leave(long player, int seat) {
    if (Long.valueOf(player).equals(observers.get(seat))) {
      observers.remove(seat);
      admissions.remove(player);
      return;
    }
    if (!Long.valueOf(player).equals(players.get(seat)))
      throw new SecurityException("seat not owned");
    if (state != null) {
      players.put(seat, -Math.addExact(Math.multiplyExact(roomId, 10L), seat + 1L));
      hostingSeats.add(seat);
    } else {
      players.remove(seat);
      readySeats.remove(seat);
      continueSeats.remove(seat);
      hostingSeats.remove(seat);
      plays.remove(seat);
      bombs.remove(seat);
      playedCardCounts.remove(seat);
      initialPatternCounts.remove(seat);
      missedOperations.remove(seat);
      totalScores.remove(seat);
      winCounts.remove(seat);
      loseCounts.remove(seat);
    }
    admissions.remove(player);
    offlineSinceEpochMillis.remove(player);
    if (player == ownerId) {
      ownerId = players.values().stream().filter(id -> id > 0).findFirst().orElse(0L);
      if (ownerId == 0L) markDissolved("EMPTY_ROOM");
    }
  }

  private void requestDissolve(long player) {
    seatOf(player);
    java.time.Instant now = java.time.Instant.ofEpochMilli(time.epochMillis());
    if (state == null) {
      if (player != ownerId)
        throw new SecurityException("only owner can directly dissolve an unstarted room");
      markDissolved("OWNER_DISSOLVED");
      return;
    }
    if (dissolveVote != null && dissolveVote.decision(now) == DissolveDecision.WAITING)
      throw new IllegalStateException("dissolve vote already active");
    dissolveVote =
        DissolveVoteState.start(
            player, new LinkedHashSet<>(players.values()), now.plus(DISSOLVE_VOTE_TIMEOUT), true);
    int threshold = family.rules().config().advancedRules().governance().offlineDissolveSeconds();
    if (threshold > 0)
      for (long participant : players.values()) {
        Long since = offlineSinceEpochMillis.get(participant);
        if (participant != player
            && since != null
            && now.toEpochMilli() - since >= threshold * 1000L)
          dissolveVote = dissolveVote.vote(participant, true, now);
      }
    if (dissolveVote.decision(now) == DissolveDecision.APPROVED)
      markDissolved("OFFLINE_VOTE_APPROVED");
    else dissolveReason = "VOTE_ACTIVE";
  }

  private void voteDissolve(long player, boolean approve) {
    if (state == null)
      throw new IllegalStateException("unstarted room does not use dissolve voting");
    if (dissolveVote == null) throw new IllegalStateException("dissolve vote is not active");
    java.time.Instant now = java.time.Instant.ofEpochMilli(time.epochMillis());
    dissolveVote = dissolveVote.vote(player, approve, now);
    DissolveDecision decision = dissolveVote.decision(now);
    if (decision == DissolveDecision.APPROVED) markDissolved("VOTE_APPROVED");
    else if (decision == DissolveDecision.REJECTED) dissolveReason = "VOTE_REJECTED";
  }

  private void markDissolved(String reason) {
    dissolved = true;
    dissolveReason = reason;
    deadline = OperationDeadline.none();
  }

  @Override
  public synchronized void participantPresence(
      long playerId, boolean online, java.time.Instant observedAt) {
    Objects.requireNonNull(observedAt);
    if (seatOfNullable(playerId) == null && observerSeatOf(playerId) == null)
      throw new IllegalStateException("player is not a room member");
    if (online) offlineSinceEpochMillis.remove(playerId);
    else offlineSinceEpochMillis.putIfAbsent(playerId, observedAt.toEpochMilli());
  }

  @Override
  public synchronized void admitParticipant(
      long playerId, int seatId, RoomAdmissionAuthority.Admission admission) {
    if (playerId <= 0 || seatId < 0 || seatId >= seatLimit)
      throw new IllegalArgumentException("invalid admission identity");
    RoomAdmissionAuthority.Admission value = Objects.requireNonNull(admission),
        normalized =
            new RoomAdmissionAuthority.Admission(
                value.ipAddress() == null ? null : value.ipAddress().strip(),
                value.latitude(),
                value.longitude());
    PdkAdvancedRules.RoomGovernance rule = family.rules().config().advancedRules().governance();
    if (rule.entryMode() != PdkAdvancedRules.EntryMode.OBSERVER || playerId == ownerId)
      enforceAdmission(playerId, normalized);
    admissions.put(playerId, normalized);
  }

  private void enforceAdmission(long playerId, RoomAdmissionAuthority.Admission value) {
    if (value == null) throw new SecurityException("admission facts required");
    PdkAdvancedRules.RoomGovernance rule = family.rules().config().advancedRules().governance();
    if (seatLimit < 3) return;
    String ip = value.ipAddress() == null ? "" : value.ipAddress().strip();
    if (rule.uniqueIpRequired()) {
      if (ip.isEmpty()) throw new SecurityException("IP address required");
      if (admissions.entrySet().stream()
          .filter(entry -> entry.getKey() != playerId)
          .map(Map.Entry::getValue)
          .map(RoomAdmissionAuthority.Admission::ipAddress)
          .filter(Objects::nonNull)
          .map(String::strip)
          .anyMatch(ip::equals)) throw new SecurityException("duplicate room IP rejected");
    }
    if (rule.gpsAdmissionRequired()) {
      if (rule.gpsMinimumDistanceMeters() <= 0)
        throw new IllegalStateException("GPS safety distance is not published");
      validateCoordinates(value);
      for (Map.Entry<Long, RoomAdmissionAuthority.Admission> entry : admissions.entrySet()) {
        if (entry.getKey() == playerId) continue;
        validateCoordinates(entry.getValue());
        if (distanceMeters(value, entry.getValue()) < rule.gpsMinimumDistanceMeters())
          throw new SecurityException("room GPS distance rejected");
      }
    }
  }

  private Map<Long, Object> admissionSnapshot() {
    Map<Long, Object> out = new LinkedHashMap<>();
    admissions.forEach(
        (player, value) ->
            out.put(
                player,
                Map.of(
                    "ipAddress",
                    Objects.requireNonNullElse(value.ipAddress(), ""),
                    "latitude",
                    value.latitude() == null ? "" : value.latitude(),
                    "longitude",
                    value.longitude() == null ? "" : value.longitude())));
    return Map.copyOf(out);
  }

  private static Map<Long, RoomAdmissionAuthority.Admission> admissionMap(Object raw) {
    Map<Long, RoomAdmissionAuthority.Admission> out = new LinkedHashMap<>();
    if (raw instanceof Map<?, ?> values)
      values.forEach(
          (key, value) -> {
            if (!(value instanceof Map<?, ?> item))
              throw new IllegalArgumentException("invalid admission snapshot");
            Object lat = item.get("latitude"),
                lon = item.get("longitude"),
                ip = item.get("ipAddress");
            out.put(
                lng(key),
                new RoomAdmissionAuthority.Admission(
                    ip == null ? null : String.valueOf(ip),
                    lat instanceof Number n ? n.doubleValue() : null,
                    lon instanceof Number n ? n.doubleValue() : null));
          });
    return out;
  }

  private static void validateCoordinates(RoomAdmissionAuthority.Admission value) {
    if (value.latitude() == null
        || value.longitude() == null
        || !Double.isFinite(value.latitude())
        || !Double.isFinite(value.longitude())
        || value.latitude() < -90
        || value.latitude() > 90
        || value.longitude() < -180
        || value.longitude() > 180) throw new SecurityException("valid GPS coordinates required");
  }

  private static double distanceMeters(
      RoomAdmissionAuthority.Admission left, RoomAdmissionAuthority.Admission right) {
    double p = Math.PI / 180,
        a = (right.latitude() - left.latitude()) * p,
        b = (right.longitude() - left.longitude()) * p,
        c =
            Math.sin(a / 2) * Math.sin(a / 2)
                + Math.cos(left.latitude() * p)
                    * Math.cos(right.latitude() * p)
                    * Math.sin(b / 2)
                    * Math.sin(b / 2);
    return 6371008.8 * 2 * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c));
  }

  private void recordLifecycleMutation() {
    stateVersion = Math.addExact(stateVersion, 1);
    events.add(Map.of("version", stateVersion, "after", authoritativeState()));
  }

  private void own(long player, int seat) {
    if (state == null) throw new IllegalStateException("round not started");
    if (!Long.valueOf(player).equals(players.get(seat)))
      throw new SecurityException("seat not owned");
  }

  private int seatOf(long id) {
    Integer s = seatOfNullable(id);
    if (s == null) throw new IllegalStateException("player not seated");
    return s;
  }

  private Integer seatOfNullable(long id) {
    return players.entrySet().stream()
        .filter(e -> e.getValue() == id)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  private Integer observerSeatOf(long id) {
    return observers.entrySet().stream()
        .filter(e -> e.getValue() == id)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  private static PokerTurnState restoreState(Map<?, ?> m) {
    Map<Integer, List<Integer>> hands = listMap(m.get("hands"));
    CardCombination previous = null;
    if (m.get("previous") instanceof Map<?, ?> p)
      previous =
          new CardCombination(
              String.valueOf(p.get("type")), num(p.get("primaryRank")), numbers(p.get("cards")));
    Set<Integer> passed = new LinkedHashSet<>();
    if (m.get("passed") instanceof Collection<?> c) c.forEach(v -> passed.add(num(v)));
    return new PokerTurnState(
        hands,
        num(m.get("currentSeat")),
        previous,
        num(m.get("previousSeat")),
        passed,
        bool(m.get("finished")),
        num(m.get("winnerSeat")));
  }

  private Map<String, Object> lastAction(
      int seat, String action, List<Integer> cards, String cardType, String operationId) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("seat", seat);
    value.put("action", action);
    value.put("cards", List.copyOf(cards));
    value.put("cardType", cardType);
    value.put("operationId", operationId);
    value.put("stateVersion", stateVersion + 1);
    value.put("playIndex", playHistory.size());
    return Map.copyOf(value);
  }

  private static Map<Integer, Map<String, Object>> lastActions(Object value) {
    Map<Integer, Map<String, Object>> out = new LinkedHashMap<>();
    if (value == null) return out;
    if (!(value instanceof Map<?, ?> items))
      throw new IllegalArgumentException("invalid last actions");
    items.forEach(
        (key, raw) -> {
          if (!(raw instanceof Map<?, ?> map))
            throw new IllegalArgumentException("invalid last action");
          Map<String, Object> item = stringMap(map);
          int seat = num(item.getOrDefault("seat", key));
          String action = String.valueOf(item.getOrDefault("action", "none"));
          List<Integer> cards =
              item.get("cards") instanceof Collection<?> ? cardList(item.get("cards")) : List.of();
          out.put(
              seat,
              Map.of(
                  "seat", seat,
                  "action", action,
                  "cards", cards,
                  "cardType", String.valueOf(item.getOrDefault("cardType", "")),
                  "operationId", String.valueOf(item.getOrDefault("operationId", "restored")),
                  "stateVersion", lng(item.getOrDefault("stateVersion", 0)),
                  "playIndex", num(item.getOrDefault("playIndex", 0))));
        });
    return out;
  }

  private static List<Map<String, Object>> playHistory(Object value) {
    if (value == null) return List.of();
    if (!(value instanceof Collection<?> items))
      throw new IllegalArgumentException("invalid play history");
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object item : items) {
      if (!(item instanceof Map<?, ?> map))
        throw new IllegalArgumentException("invalid play history item");
      Map<String, Object> entry = stringMap(map);
      int index = num(entry.get("playIndex")), seat = num(entry.get("seat"));
      List<Integer> cards = numbers(entry.get("cards"));
      if (index != out.size() + 1 || seat < 0)
        throw new IllegalArgumentException("invalid play history order");
      out.add(
          Map.of(
              "playIndex",
              index,
              "seat",
              seat,
              "type",
              String.valueOf(entry.get("type")),
              "cards",
              List.copyOf(cards)));
    }
    return List.copyOf(out);
  }

  private static Map<Integer, Long> longMap(Object v) {
    Map<Integer, Long> o = new LinkedHashMap<>();
    if (v instanceof Map<?, ?> m) m.forEach((k, x) -> o.put(num(k), lng(x)));
    return o;
  }

  private static Map<Long, Long> longLongMap(Object v) {
    Map<Long, Long> o = new LinkedHashMap<>();
    if (v instanceof Map<?, ?> m) m.forEach((k, x) -> o.put(lng(k), lng(x)));
    return o;
  }

  private static Map<Integer, Integer> intMap(Object v) {
    Map<Integer, Integer> o = new LinkedHashMap<>();
    if (v instanceof Map<?, ?> m) m.forEach((k, x) -> o.put(num(k), num(x)));
    return o;
  }

  private static Map<Integer, List<Integer>> listMap(Object v) {
    Map<Integer, List<Integer>> o = new LinkedHashMap<>();
    if (v instanceof Map<?, ?> m) m.forEach((k, x) -> o.put(num(k), cardList(x)));
    return o;
  }

  private static List<CardCombination> cardCombinations(Object v) {
    if (!(v instanceof Collection<?> values))
      throw new IllegalStateException("missing played card history");
    List<CardCombination> out = new ArrayList<>();
    for (Object value : values) {
      if (value instanceof CardCombination c) out.add(c);
      else if (value instanceof Map<?, ?> m)
        out.add(
            new CardCombination(
                String.valueOf(m.get("type")), num(m.get("primaryRank")), numbers(m.get("cards"))));
      else throw new IllegalArgumentException("invalid played card history");
    }
    return List.copyOf(out);
  }

  private static Map<String, Object> stringMap(Map<?, ?> m) {
    Map<String, Object> o = new LinkedHashMap<>();
    m.forEach((k, v) -> o.put(String.valueOf(k), v));
    return o;
  }

  private static List<Integer> cardList(Object v) {
    if (!(v instanceof Collection<?> c)) throw new IllegalArgumentException("cards required");
    return c.stream().map(PokerAuthoritativeSession::num).toList();
  }

  private static List<Integer> numbers(Object v) {
    List<Integer> c = cardList(v);
    if (c.isEmpty()) throw new IllegalArgumentException("cards required");
    return c;
  }

  private static int num(Object v) {
    return v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v));
  }

  private static long lng(Object v) {
    return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
  }

  private static boolean bool(Object v) {
    return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
  }

  private static long player(String v) {
    long x = Long.parseLong(v);
    if (x <= 0) throw new IllegalArgumentException("invalid player");
    return x;
  }

  private static String operation(GameCommandRequest r) {
    return leaf(r.msgId());
  }

  private static CommandPayload commandBody(GameCommandRequest r) {
    return r.body();
  }

  private static String leaf(String id) {
    String v = id.substring(id.lastIndexOf('.') + 1);
    return v.endsWith("_req") ? v.substring(0, v.length() - 4) : v;
  }
}
