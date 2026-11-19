package com.aoo.bcg.poker.cx;

import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import com.aoo.bcg.common.room.AuthoritativeRandomSeatAllocator;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-owned真人状态机；不包含机器人、聊天、定位或支付逻辑。 */
public final class CxSession implements AuthoritativeGameSession, CommonRoomPublicViewProvider, CarryBillingEffectSource {
    private static final Logger LOG=LoggerFactory.getLogger(CxSession.class);
    private static final List<Integer> CX_DECK=List.of(202,402,203,104,204,304,404,105,305,106,206,306,406,107,207,307,407,108,208,308,408,109,309,110,210,310,410,111,311,212,412,506);
    private static final AuthoritativeRandomSeatAllocator SEAT_ALLOCATOR=new AuthoritativeRandomSeatAllocator();
    private static final long SEAT_RANDOM_DOMAIN=-964087935L;
    public enum Phase { WAITING, BASE_AND_MANGO, DEALING, BETTING, ADD_CARD, SPLITTING, REVEAL, ROUND_SETTLEMENT, FINISHED }
    public enum BetAction { DROP, FOLLOW, REST, RAISE, ALL_IN }
    private final long roomId, ownerId, seed; private final CxRules rules;
    private long seatRandomSeed,seatRandomSequence;
    private final AuthoritativeTimeSource time;
    private final CxHandEvaluator evaluator=new CxHandEvaluator();
    private final Map<Integer,Long> players=new LinkedHashMap<>(); private final Set<Integer> ready=new LinkedHashSet<>();
    private final Map<Integer,List<Integer>> hands=new LinkedHashMap<>(); private final Map<Integer,Long> scores=new LinkedHashMap<>(),carryScores=new LinkedHashMap<>();
    private final Map<Integer,Integer> committed=new LinkedHashMap<>(); private final Set<Integer> dropped=new LinkedHashSet<>(), split=new LinkedHashSet<>(),threeFlowerSeats=new LinkedHashSet<>();
    private final Map<Integer,Integer> bases=new LinkedHashMap<>(),mangos=new LinkedHashMap<>(),bets=new LinkedHashMap<>(),presetBetTypes=new LinkedHashMap<>();
    private final Map<Integer,Integer> mangoCursors=new LinkedHashMap<>();
    private final Map<Integer,Integer> winCounts=new LinkedHashMap<>(),loseCounts=new LinkedHashMap<>();
    private final Map<Integer,Long> restartCarryScores=new LinkedHashMap<>();
    private static final class CarryAttempt {
        final Proposal proposal; final String id; boolean applied,used,terminated;
        CarryAttempt(Proposal proposal){this.proposal=proposal;this.id=CarryBillingEffectSource.attemptId(proposal.roomId(),proposal.authenticatedActorId(),proposal.originalRequestId(),proposal.kind());}
    }
    private final Map<String,CarryAttempt> carryAttempts=new LinkedHashMap<>();
    private final Map<Integer,String> pendingCarryBySeat=new LinkedHashMap<>(),appliedCarryBySeat=new LinkedHashMap<>();
    private final List<Effect> carryEffects=new ArrayList<>();
    private long carryOrdinal,carryAckOrdinal; private String carryAckHash="";
    private static final int SCENE_ATTEMPT_LIMIT=4096;
    private enum SceneStatus { PENDING, APPLIED, FIRST_OPENING_COMMITTED, ABORTED }
    private static final class SceneAttempt {
        final String request; final long actor,target,acceptedVersion; final int seat;
        SceneStatus status=SceneStatus.PENDING; long appliedVersion=-1,openingVersion=-1; String abortReason="";
        SceneAttempt(String request,long actor,int seat,long target,long acceptedVersion){this.request=request;this.actor=actor;this.seat=seat;this.target=target;this.acceptedVersion=acceptedVersion;}
    }
    private long sceneOrdinal;
    private final Map<String,SceneAttempt> sceneAttempts=new LinkedHashMap<>();
    private final Map<Integer,String> pendingSceneBySeat=new LinkedHashMap<>();
    private final Set<String> activeSceneAttempts=new LinkedHashSet<>();


    private final Set<Integer> playedSeats=new LinkedHashSet<>(),roundSeats=new LinkedHashSet<>();
    private final Map<Integer,BetAction> lastBetActions=new LinkedHashMap<>(); private final Set<Integer> allInSeats=new LinkedHashSet<>();
    private final Set<String> operations=new LinkedHashSet<>(); private final Set<Integer> phaseActedSeats=new LinkedHashSet<>(); private final List<Object> events=new ArrayList<>(),replayEvents=new ArrayList<>();
    private final OperationDeadlineArbiter arbiter=new OperationDeadlineArbiter(); private Phase phase=Phase.WAITING;
    private final Map<Integer,Integer> timeoutCounts=new LinkedHashMap<>(),splitDelayCounts=new LinkedHashMap<>(); private final Set<Integer> hostingSeats=new LinkedHashSet<>();
    private final Map<Integer,Long> splitDeadlineEpochMillis=new LinkedHashMap<>();
    private final Map<Integer,Long> seatRetentionDeadlineEpochMillis=new LinkedHashMap<>();
    private OperationDeadline operationDeadline=OperationDeadline.none();
    private final List<Integer> deck=new ArrayList<>(); private int deckCursor;
    private int round,currentSeat=-1,bankerSeat=-1,betTarget,upMangoLevel,roundSettlementTriggerSeat=-1; private String lastMangoEvent="NONE"; private long stateVersion,roomStartedAtEpochMillis,roomEndsAtEpochMillis,terminalDeadlineEpochMillis,roundSettlementDeadlineEpochMillis,mangoPool; private Map<Long,Long> lastDelta=Map.of();
    public CxSession(long roomId,long ownerId,long seed,CxRules rules){this(roomId,ownerId,seed,rules,AuthoritativeTimeSource.systemUtc());}
    CxSession(long roomId,long ownerId,long seed,CxRules rules,AuthoritativeTimeSource time){this(roomId,ownerId,seed,rules,time,SeededGameRandomSource.create().seed());}
    CxSession(long roomId,long ownerId,long seed,CxRules rules,AuthoritativeTimeSource time,long seatRandomSeed){this.roomId=roomId;this.ownerId=ownerId;this.seed=seed;this.rules=Objects.requireNonNull(rules);this.time=Objects.requireNonNull(time);this.seatRandomSeed=seatRandomSeed;}
    static long seatDrawSeed(long seatRandomSeed,long sequence){long value=seatRandomSeed^SEAT_RANDOM_DOMAIN^Long.rotateLeft(sequence,21);value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
    /** Atomically changes one authenticated spectator into a seated, auto-ready player. */
    public synchronized void sit(int ignoredSeat,long playerId,String op){sit(playerId,100L,op);}
    public synchronized void sit(int ignoredSeat,long playerId,long carryScore,String op){sit(playerId,carryScore,op);}
    public synchronized void sit(long playerId,long carryScore,String op){
        Integer occupiedSeat=validateSitCarry(playerId,carryScore,op);
        if(occupiedSeat!=null){
            carryScores.put(occupiedSeat,carryScore);scores.put(occupiedSeat,carryScore);seatRetentionDeadlineEpochMillis.remove(occupiedSeat);changed(op,"sit-confirm",occupiedSeat);applyCarry(requestCarry(Kind.CONFIRM,occupiedSeat,playerId,carryScore,op));
            LOG.info("[CX] reserved seat confirmed roomId={} playerId={} seatId={} carryScore={} operationId={} stateVersion={}",roomId,playerId,occupiedSeat,carryScore,op,stateVersion);
            if(phase==Phase.WAITING&&fundedSeatCount()>=rules.startPlayers())startRound();
            else if(phase==Phase.ROUND_SETTLEMENT)scheduleNextRoundWhenFunded(2_000L);
            return;
        }
        int seat=SEAT_ALLOCATOR.allocate(rules.maxPlayers(),Set.copyOf(players.keySet()),
                new SeededGameRandomSource(seatDrawSeed(seatRandomSeed,seatRandomSequence)));
        boolean waiting=phase==Phase.WAITING;
        players.put(seat,playerId);seatRandomSequence++;carryScores.put(seat,carryScore);scores.put(seat,carryScore);winCounts.put(seat,0);loseCounts.put(seat,0);ready.add(seat);changed(op,"sit",seat);applyCarry(requestCarry(Kind.INITIAL,seat,playerId,carryScore,op));
        if(carryScore==0)seatRetentionDeadlineEpochMillis.put(seat,Math.addExact(time.epochMillis(),Duration.ofSeconds(rules.seatRetentionSeconds()).toMillis()));
        LOG.info("[CX] sit accepted roomId={} playerId={} seatId={} carryScore={} operationId={} stateVersion={}",roomId,playerId,seat,carryScore,op,stateVersion);
        if(waiting&&fundedSeatCount()>=rules.startPlayers())startRound();
        else if(!waiting)scheduleNextRoundWhenFunded(2_000L);
    }
    private RuntimeException sitRejected(String code,int seat,long playerId,String op,boolean invalid){LOG.warn("[CX] sit rejected code={} roomId={} playerId={} seatId={} operationId={} stateVersion={}",code,roomId,playerId,seat,op,stateVersion);String message="[CX] "+code+" roomId="+roomId+" playerId="+playerId+" seatId="+seat;return invalid?new IllegalArgumentException(message):new IllegalStateException(message);}
    /** Single legacy-read bridge. New clients must send sit_req instead of ready_req. */
    private void legacyReady(long playerId,String op){sit(playerId,0L,op);}
    /** XQP PREBET: a non-current player may toggle automatic REST(1) or DROP(2). */
    public synchronized void presetBet(int seat,int type,String op){mutating(op);requireSeat(seat);if(phase!=Phase.BETTING||currentSeat==seat||dropped.contains(seat)||allInSeats.contains(seat)||type<0||type>2)throw new IllegalStateException("CX preset bet unavailable");presetBetTypes.put(seat,type);changed(op,"presetBet:"+type,seat);}
    public synchronized void bet(int seat,BetAction action,int amount,String op){mutating(op);requireTurn(seat,Phase.BETTING);Objects.requireNonNull(action,"action");int previous=bets.getOrDefault(seat,0);long available=availableScore(seat);if(rules.eachPlayerMustFollow()&&hands.get(seat).size()==2&&previous==0&&(action==BetAction.DROP||action==BetAction.REST))throw new IllegalArgumentException("CX first follow required");switch(action){case DROP->dropped.add(seat);case REST->{if(!mayRest(seat))throw new IllegalArgumentException("CX rest unavailable");}case FOLLOW->{int value=Math.max(0,betTarget-previous);if(value>available)throw new IllegalArgumentException("CX cannot follow: insufficient score");bets.put(seat,previous+value);committed.merge(seat,value,Integer::sum);}case RAISE->{int minimum=minimumRaiseTarget();int value=Math.subtractExact(amount,previous);if(amount<minimum||amount<=previous||value>available)throw new IllegalArgumentException("invalid CX raise");bets.put(seat,amount);committed.merge(seat,value,Integer::sum);betTarget=amount;phaseActedSeats.clear();}case ALL_IN->{long allInTotal=Math.addExact(previous,available);if(allInTotal<=previous||allInTotal>Integer.MAX_VALUE)throw new IllegalArgumentException("invalid CX all-in");int authoritativeAmount=(int)allInTotal;int value=authoritativeAmount-previous;bets.put(seat,authoritativeAmount);committed.merge(seat,value,Integer::sum);allInSeats.add(seat);if(authoritativeAmount>betTarget){betTarget=authoritativeAmount;phaseActedSeats.clear();}}}lastBetActions.put(seat,action);changed(op,"bet:"+action,seat);advanceBetting(seat);openOperationWindow();}
    public synchronized void addCard(int seat,String op){mutating(op);requireTurn(seat,Phase.ADD_CARD);List<Integer> hand=new ArrayList<>(hands.get(seat));hand.add(drawCard());hands.put(seat,List.copyOf(hand));if(hand.size()==3&&evaluator.isThreeFlower(hand))threeFlowerSeats.add(seat);changed(op,"addCard",seat);if(activeSeats().size()<=1){phase=Phase.REVEAL;settle();return;}advanceOr(seat,hand.size()>=4?Phase.SPLITTING:Phase.BETTING);if(phase==Phase.BETTING)beginBettingPhase();else if(phase==Phase.SPLITTING)currentSeat=activeSeats().stream().filter(s->!split.contains(s)).findFirst().orElse(-1);openOperationWindow();}
    public synchronized void split(int seat,List<Integer> ordered,String op){mutating(op);requireSeat(seat);if(phase!=Phase.SPLITTING||dropped.contains(seat)||threeFlowerSeats.contains(seat)||split.contains(seat)||time.epochMillis()>=splitDeadlineEpochMillis.getOrDefault(seat,0L)||!sameCards(hands.get(seat),ordered))throw new IllegalArgumentException("invalid CX split");List<Integer>canonical=evaluator.bigLeftOrder(ordered,rules.earthNineKing());hands.put(seat,canonical);split.add(seat);splitDeadlineEpochMillis.remove(seat);changed(op,"split",seat);LOG.info("[CX] split canonicalized big-left roomId={} playerId={} seatId={} operationId={} stateVersion={} bigCards={} smallCards={}",roomId,players.get(seat),seat,op,stateVersion,canonical.subList(0,2),canonical.subList(2,4));if(split.containsAll(activeSeats())){phase=Phase.REVEAL;settle();return;}currentSeat=activeSeats().stream().filter(s->!split.contains(s)).findFirst().orElse(-1);}
    /** XQP delay-split equivalent: extend only the caller's live split window and enforce the configured limit. */
    public synchronized void delaySplit(int seat,String op){mutating(op);requireSeat(seat);long deadline=splitDeadlineEpochMillis.getOrDefault(seat,0L);if(phase!=Phase.SPLITTING||split.contains(seat)||threeFlowerSeats.contains(seat)||deadline<=time.epochMillis())throw new IllegalStateException("CX split delay unavailable");int used=splitDelayCounts.getOrDefault(seat,0);if(used>=rules.splitExtensionLimit())throw new IllegalStateException("CX split delay limit reached");splitDelayCounts.put(seat,used+1);splitDeadlineEpochMillis.put(seat,Math.addExact(deadline,Duration.ofSeconds(rules.splitExtensionSeconds()).toMillis()));changed(op,"delaySplit",seat);}
    private void autoSplit(int seat,String op){mutating(op);requireSeat(seat);if(phase!=Phase.SPLITTING||split.contains(seat)||threeFlowerSeats.contains(seat))throw new IllegalStateException("CX auto split unavailable");List<Integer>canonical=legalAutoSplit(hands.get(seat));hands.put(seat,canonical);split.add(seat);splitDeadlineEpochMillis.remove(seat);changed(op,"autoSplit",seat);LOG.info("[CX] auto split canonicalized big-left roomId={} playerId={} seatId={} operationId={} stateVersion={} bigCards={} smallCards={}",roomId,players.get(seat),seat,op,stateVersion,canonical.subList(0,2),canonical.subList(2,4));if(split.containsAll(activeSeats())){phase=Phase.REVEAL;settle();return;}currentSeat=activeSeats().stream().filter(s->!split.contains(s)).findFirst().orElse(-1);}
    public synchronized void timeout(int seat,String op){
        long now=time.epochMillis();
        if(phase==Phase.ROUND_SETTLEMENT&&seat==roundSettlementTriggerSeat&&now>=roundSettlementDeadlineEpochMillis){mutating(op);changed(op,"auto-next-round",seat);startRound();return;}
        long seatDeadline=seatRetentionDeadlineEpochMillis.getOrDefault(seat,0L);
        if(seatDeadline>0&&now>=seatDeadline){mutating(op);changed(op,"seat-retention-expired",seat);removeSeatOccupancyState(seat);LOG.info("[CX] depleted seat expired roomId={} seatId={} operationId={} stateVersion={}",roomId,seat,op,stateVersion);return;}
        boolean splitExpired=phase==Phase.SPLITTING&&now>=splitDeadlineEpochMillis.getOrDefault(seat,Long.MAX_VALUE);if(!splitExpired&&(!operationDeadline.open()||operationDeadline.seatId()!=seat||!operationDeadline.expired(time)))throw new IllegalStateException("no expired matching CX operation deadline");int count=timeoutCounts.merge(seat,1,Integer::sum);if(count>=rules.autoOperateAfterTimeouts())hostingSeats.add(seat);LOG.info("[CX] operation timeout roomId={} playerId={} seatId={} operationId={} stateVersion={} timeoutCount={} hosting={}",roomId,players.get(seat),seat,op,stateVersion,count,hostingSeats.contains(seat));switch(phase){case BETTING->{boolean mustFollow=rules.eachPlayerMustFollow()&&hands.get(seat).size()==2&&bets.getOrDefault(seat,0)==0;if(mustFollow)bet(seat,Math.max(0,betTarget-bets.getOrDefault(seat,0))<=availableScore(seat)?BetAction.FOLLOW:BetAction.ALL_IN,0,op);else if(mayRest(seat))bet(seat,BetAction.REST,0,op);else bet(seat,BetAction.DROP,0,op);}case SPLITTING->autoSplit(seat,op);default->throw new IllegalStateException("no CX timeout action");}}
    private void startRound(){
        long now=time.epochMillis();
        if(roomStartedAtEpochMillis==0){roomStartedAtEpochMillis=now;roomEndsAtEpochMillis=rules.endMode()==CxRules.EndMode.DURATION_MINUTES?now+Duration.ofMinutes(rules.endValue()).toMillis():0;}
        if(rules.endMode()==CxRules.EndMode.ROUND_COUNT&&round>=rules.endValue()){finishRoom();return;}
        roundSettlementDeadlineEpochMillis=0;
        roundSettlementTriggerSeat=-1;
        roundSeats.clear();
        for(int seat:players.keySet())if(round==0||canFundNextRound(seat))roundSeats.add(seat);
        if(roundSeats.size()<2){finishRoom();return;}
        bankerSeat=nextBankerSeat(roundSeats);
        Set<Integer>nextRoundSeats=new LinkedHashSet<>(roundSeats);
        resetRoundDisplayState();
        roundSeats.addAll(nextRoundSeats);
        round++;betTarget=0;deck.clear();deck.addAll(CX_DECK);Collections.shuffle(deck,new Random(seed+round));deckCursor=0;
        for(int seat:roundSeats){int mango=requiredMango(seat);int openingCommit=Math.addExact(1,mango);if(scores.getOrDefault(seat,0L)<=openingCommit)continue;bases.put(seat,1);mangos.put(seat,mango);committed.put(seat,openingCommit);playedSeats.add(seat);hands.put(seat,List.of(drawCard(),drawCard()));useCarry(seat);}
        roundSeats.retainAll(hands.keySet());if(roundSeats.size()<2){finishRoom();return;}events.add(Map.of("type","deal","stateVersion",stateVersion+1,"bankerSeat",bankerSeat));beginBettingPhase();stateVersion++;commitSceneOpening();openOperationWindow();LOG.info("[CX] round banker selected roomId={} roundNo={} bankerSeat={} bankerPlayerId={} stateVersion={}",roomId,round,bankerSeat,players.get(bankerSeat),stateVersion);
    }
    /** First round is random; later rounds rotate to the next occupied seat counterclockwise. */
    private int nextBankerSeat(Set<Integer> eligible){List<Integer>seats=eligible.stream().sorted().toList();if(seats.isEmpty())return -1;if(round==0||bankerSeat<0)return seats.get(new Random(seed^roomStartedAtEpochMillis).nextInt(seats.size()));for(int index=seats.size()-1;index>=0;index--){int seat=seats.get(index);if(seat<bankerSeat)return seat;}return seats.getLast();}
    private void finishRoom(){openTerminalRetention();stateVersion++;LOG.info("[CX] terminal retention opened roomId={} roundNo={} stateVersion={} deadlineEpochMillis={} playerCount={}",roomId,round,stateVersion,terminalDeadlineEpochMillis,players.size());}
    /** Initializes the single authoritative terminal window without owning the caller's version transition. */
    private void openTerminalRetention(){abortUnopenedScene("TERMINAL");terminateAllUnusedCarry();phase=Phase.FINISHED;currentSeat=-1;operationDeadline=OperationDeadline.none();restartCarryScores.clear();terminalDeadlineEpochMillis=Math.addExact(time.epochMillis(),Duration.ofSeconds(rules.seatRetentionSeconds()).toMillis());}
    private void restart(int seat,long playerId,long carryScore,String op){
        terminalMutation(seat,playerId,carryScore,op);validateSceneRequest(op);new Proposal(op,roomId,playerId,seat,carryScore,Kind.RESTART);restartCarryScores.put(seat,carryScore);changed(op,"restart-ready",seat);requestCarry(Kind.RESTART,seat,playerId,carryScore,op);acceptSceneRequest(op,playerId,seat);
        LOG.info("[CX] terminal continue accepted roomId={} playerId={} seatId={} carryScore={} operationId={} stateVersion={} readyCount={}",roomId,playerId,seat,carryScore,op,stateVersion,restartCarryScores.size());
        if(restartCarryScores.size()<rules.startPlayers())return;
        Set<Integer>retained=new LinkedHashSet<>(restartCarryScores.keySet());
        players.keySet().removeIf(s->!retained.contains(s));ready.retainAll(retained);carryScores.keySet().retainAll(retained);scores.keySet().retainAll(retained);winCounts.keySet().retainAll(retained);loseCounts.keySet().retainAll(retained);
        restartCarryScores.forEach((s,value)->{carryScores.put(s,value);scores.put(s,value);winCounts.put(s,0);loseCounts.put(s,0);String attempt=pendingCarryBySeat.get(s);if(attempt!=null)applyCarry(carryAttempts.get(attempt));});
        applyPendingScene();resetRoomSeries();startRound();
    }
    private void rebuy(int seat,long playerId,long carryScore,String op){validateRebuyCarry(seat,playerId,carryScore,op);long minimumExclusive=Math.addExact(1L,requiredMango(seat));carryScores.put(seat,carryScore);scores.put(seat,carryScore);seatRetentionDeadlineEpochMillis.remove(seat);scheduleNextRoundWhenFunded(500L);changed(op,"rebuy",seat);applyCarry(requestCarry(Kind.REBUY,seat,playerId,carryScore,op));LOG.info("[CX] depleted seat rebuy accepted roomId={} playerId={} seatId={} carryScore={} minimumExclusive={} operationId={} stateVersion={}",roomId,playerId,seat,carryScore,minimumExclusive,op,stateVersion);}
    private void scheduleNextRoundWhenFunded(long delayMillis){if(phase!=Phase.ROUND_SETTLEMENT||fundedSeatCount()<rules.startPlayers())return;roundSettlementTriggerSeat=players.keySet().stream().filter(this::canFundNextRound).min(Integer::compareTo).orElse(-1);roundSettlementDeadlineEpochMillis=roundSettlementTriggerSeat<0?0:Math.addExact(time.epochMillis(),delayMillis);}
    private void stand(int seat,long playerId,String op){if(players.get(seat)==null||players.get(seat)!=playerId)throw new IllegalArgumentException("[CX] authenticated seat mismatch");if(phase!=Phase.WAITING&&phase!=Phase.ROUND_SETTLEMENT&&phase!=Phase.FINISHED)throw new IllegalStateException("[CX] stand unavailable during active hand");if(phase==Phase.FINISHED)terminalMutation(seat,playerId,0L,op);else mutating(op);changed(op,"stand",seat);removeSeatOccupancyState(seat);removeSeatDisplayState(seat);LOG.info("[CX] stand accepted roomId={} playerId={} seatId={} operationId={} phase={} stateVersion={} playerCount={}",roomId,playerId,seat,op,phase,stateVersion,players.size());}
    private void removeSeatOccupancyState(int seat){abortSceneRequest(pendingSceneBySeat.remove(seat),"SEAT_REMOVED");terminateUnusedCarry(pendingCarryBySeat.remove(seat));terminateUnusedCarry(appliedCarryBySeat.remove(seat));players.remove(seat);ready.remove(seat);scores.remove(seat);carryScores.remove(seat);restartCarryScores.remove(seat);winCounts.remove(seat);loseCounts.remove(seat);seatRetentionDeadlineEpochMillis.remove(seat);hostingSeats.remove(seat);}
    private void removeSeatDisplayState(int seat){hands.remove(seat);committed.remove(seat);bases.remove(seat);mangos.remove(seat);bets.remove(seat);presetBetTypes.remove(seat);mangoCursors.remove(seat);lastBetActions.remove(seat);timeoutCounts.remove(seat);splitDelayCounts.remove(seat);splitDeadlineEpochMillis.remove(seat);dropped.remove(seat);split.remove(seat);threeFlowerSeats.remove(seat);playedSeats.remove(seat);roundSeats.remove(seat);allInSeats.remove(seat);phaseActedSeats.remove(seat);}
    private void resetRoundDisplayState(){hands.clear();committed.clear();bases.clear();mangos.clear();bets.clear();presetBetTypes.clear();mangoCursors.clear();lastBetActions.clear();timeoutCounts.clear();splitDelayCounts.clear();splitDeadlineEpochMillis.clear();dropped.clear();split.clear();threeFlowerSeats.clear();playedSeats.clear();roundSeats.clear();allInSeats.clear();phaseActedSeats.clear();}
    private void terminalMutation(int seat,long playerId,long carryScore,String op){if(op==null||op.isBlank()||operations.contains(op)||phase!=Phase.FINISHED)throw new IllegalStateException("[CX] terminal action unavailable");if(players.get(seat)==null||players.get(seat)!=playerId)throw new IllegalArgumentException("[CX] authenticated seat mismatch");if(time.epochMillis()>=terminalDeadlineEpochMillis)throw new IllegalStateException("[CX] terminal retention expired");if(carryScore<0||carryScore>1_000_000_000L)throw new IllegalArgumentException("[CX] invalid carry score");}
    private void resetRoomSeries(){for(String id:List.copyOf(pendingCarryBySeat.values()))terminateUnusedCarry(id);pendingCarryBySeat.clear();round=0;roomStartedAtEpochMillis=0;roomEndsAtEpochMillis=0;terminalDeadlineEpochMillis=0;roundSettlementDeadlineEpochMillis=0;roundSettlementTriggerSeat=-1;mangoPool=0;upMangoLevel=0;lastMangoEvent="NONE";lastDelta=Map.of();playedSeats.clear();roundSeats.clear();hands.clear();committed.clear();bases.clear();mangos.clear();bets.clear();presetBetTypes.clear();mangoCursors.clear();lastBetActions.clear();allInSeats.clear();dropped.clear();split.clear();threeFlowerSeats.clear();phaseActedSeats.clear();timeoutCounts.clear();splitDelayCounts.clear();hostingSeats.clear();splitDeadlineEpochMillis.clear();seatRetentionDeadlineEpochMillis.clear();deck.clear();deckCursor=0;betTarget=0;currentSeat=-1;bankerSeat=-1;restartCarryScores.clear();phase=Phase.WAITING;operationDeadline=OperationDeadline.none();}
    private void beginBettingPhase(){phase=Phase.BETTING;phaseActedSeats.clear();lastBetActions.clear();int cards=activeSeats().isEmpty()?2:hands.get(activeSeats().getFirst()).size();int previousMax=bets.values().stream().mapToInt(Integer::intValue).max().orElse(0);betTarget=cards<=2?rules.openingBet():Math.multiplyExact(Math.max(1,previousMax),2);for(int seat:threeFlowerSeats)if(!dropped.contains(seat)&&!allInSeats.contains(seat)){phaseActedSeats.add(seat);lastBetActions.put(seat,BetAction.REST);}currentSeat=activeSeats().stream().filter(s->!allInSeats.contains(s)&&!threeFlowerSeats.contains(s)).findFirst().orElse(-1);applyPresetIfPossible();if(currentSeat<0&&shouldAutoDealAfterAllIn(activeSeats()))autoDealToSplit(activeSeats());}
    private void applyPresetIfPossible(){while(phase==Phase.BETTING&&currentSeat>=0){int type=presetBetTypes.getOrDefault(currentSeat,0);if(type==0)break;if(type==1&&mayRest(currentSeat)){presetBetTypes.put(currentSeat,0);bet(currentSeat,BetAction.REST,0,"auto-prebet-"+round+"-"+stateVersion+"-"+currentSeat);}else if(type==2){presetBetTypes.put(currentSeat,0);bet(currentSeat,BetAction.DROP,0,"auto-prebet-"+round+"-"+stateVersion+"-"+currentSeat);}else break;}}
    private boolean mayRest(int seat){if(hands.get(seat).size()==2&&!rules.firstRoundCanRest())return false;List<Integer>acted=new ArrayList<>(phaseActedSeats);for(int i=acted.size()-1;i>=0;i--){int prior=acted.get(i);if(dropped.contains(prior))continue;return lastBetActions.get(prior)==BetAction.REST;}return true;}
    private long availableScore(int seat){return Math.max(0L,scores.getOrDefault(seat,0L)-committed.getOrDefault(seat,0));}
    private void advanceBetting(int actedSeat){phaseActedSeats.add(actedSeat);List<Integer>active=activeSeats();if(active.size()<=1){phase=Phase.REVEAL;boolean beat=rules.beatMango()&&!active.isEmpty()&&hands.values().stream().allMatch(hand->hand.size()==2)&&lastBetActions.get(active.getFirst())!=BetAction.REST;if(beat)raiseMango("BEAT");settle(beat);return;}boolean allRest=active.stream().allMatch(s->phaseActedSeats.contains(s)&&lastBetActions.get(s)==BetAction.REST);if(allRest){phase=Phase.REVEAL;if(rules.restMango())settleRestMango();else settleRestWithoutMango();return;}if(shouldAutoDealAfterAllIn(active)){autoDealToSplit(active);return;}List<Integer>remaining=active.stream().filter(s->!allInSeats.contains(s)&&!threeFlowerSeats.contains(s)).filter(s->!phaseActedSeats.contains(s)||bets.getOrDefault(s,0)<betTarget).toList();if(remaining.isEmpty()){if(active.stream().allMatch(seat->hands.get(seat).size()>=4))beginSplitting(active);else dealNextCardRound(active);return;}currentSeat=remaining.stream().filter(s->s>actedSeat).findFirst().orElse(remaining.getFirst());applyPresetIfPossible();}
    /** XQP addPoker(1): a closed betting round makes the server deal one card to every survivor. */
    private void dealNextCardRound(List<Integer> active){phase=Phase.ADD_CARD;phaseActedSeats.clear();for(int seat:active){List<Integer>hand=new ArrayList<>(hands.get(seat));if(hand.size()<4){hand.add(drawCard());hands.put(seat,List.copyOf(hand));}if(hand.size()>=3&&evaluator.isThreeFlower(hand))threeFlowerSeats.add(seat);}lastBetActions.clear();beginBettingPhase();}
    /** XQP checkOver/addPoker(Integer.MAX_VALUE): all-in closes action and deals every survivor to four cards. */
    private boolean shouldAutoDealAfterAllIn(List<Integer> active){
        if(allInSeats.isEmpty())return false;
        List<Integer>nonAllIn=active.stream().filter(s->!allInSeats.contains(s)).toList();
        if(nonAllIn.isEmpty())return true;
        int maxAllIn=allInSeats.stream().filter(active::contains).mapToInt(s->bets.getOrDefault(s,0)).max().orElse(0);
        return nonAllIn.size()==1&&bets.getOrDefault(nonAllIn.getFirst(),0)>=maxAllIn;
    }
    private void autoDealToSplit(List<Integer> active){
        for(int seat:active){
            List<Integer>hand=new ArrayList<>(hands.get(seat));
            while(hand.size()<4){hand.add(drawCard());if(hand.size()==3&&evaluator.isThreeFlower(hand))threeFlowerSeats.add(seat);}
            hands.put(seat,List.copyOf(hand));
        }
        phaseActedSeats.clear();
        List<Integer>splitCandidates=activeSeats();
        if(splitCandidates.isEmpty()){phase=Phase.REVEAL;settle();return;}
        beginSplitting(splitCandidates);
    }
    private void beginSplitting(List<Integer> candidates){phase=Phase.SPLITTING;operationDeadline=OperationDeadline.none();long deadline=Math.addExact(time.epochMillis(),Duration.ofSeconds(rules.splitSeconds()).toMillis());for(int seat:candidates){if(threeFlowerSeats.contains(seat)){split.add(seat);continue;}splitDeadlineEpochMillis.put(seat,deadline);}if(split.containsAll(activeSeats())){phase=Phase.REVEAL;settle();return;}currentSeat=candidates.stream().filter(s->!split.contains(s)).findFirst().orElse(-1);}
    private List<Integer> legalAutoSplit(List<Integer> cards){for(int a=0;a<cards.size();a++)for(int b=a+1;b<cards.size();b++){List<Integer>ordered=new ArrayList<>();ordered.add(cards.get(a));ordered.add(cards.get(b));for(int i=0;i<cards.size();i++)if(i!=a&&i!=b)ordered.add(cards.get(i));try{return evaluator.bigLeftOrder(ordered,rules.earthNineKing());}catch(IllegalArgumentException ignored){}}throw new IllegalStateException("CX has no legal split");}
    private void settle(){settle(false);}
    private void settle(boolean keepRaisedMango){List<CxSettlementEngine.Player>input=new ArrayList<>();for(int seat:roundSeats){long playerId=players.get(seat);boolean drop=dropped.contains(seat),flower=threeFlowerSeats.contains(seat)||evaluator.isThreeFlower(hands.get(seat));CxHandEvaluator.SplitRank rank=drop||flower||hands.get(seat).size()!=4?null:evaluator.split(hands.get(seat),rules.earthNineKing());input.add(new CxSettlementEngine.Player(seat,playerId,bases.getOrDefault(seat,1),mangos.getOrDefault(seat,rules.mangoScore()),bets.getOrDefault(seat,0),drop,flower,rank));}CxSettlementEngine.Result result=new CxSettlementEngine().settle(input,rules.headBigKeepsBase(),mangoPool);mangoPool=result.mangoPool();if(!keepRaisedMango)clearRaisedMango();completeSettlement(result.scoreDelta());}
    private void settleRestMango(){raiseMango("REST");Map<Long,Long>delta=new LinkedHashMap<>();long contributed=0;for(int seat:roundSeats){long mango=mangos.getOrDefault(seat,rules.mangoScore());delta.put(players.get(seat),-mango);contributed=Math.addExact(contributed,mango);}mangoPool=Math.addExact(mangoPool,contributed);completeSettlement(delta);}
    private void settleRestWithoutMango(){lastMangoEvent="REST_NO_RAISE";completeSettlement(roundSeats.stream().collect(java.util.stream.Collectors.toMap(players::get,s->0L,(a,b)->a,LinkedHashMap::new)));}
    private void raiseMango(String event){lastMangoEvent=event;if(rules.mangoRaise()>0)upMangoLevel=Math.min(rules.mangoRaise(),upMangoLevel+1);roundSeats.forEach(seat->mangoCursors.put(seat,Math.max(0,upMangoLevel-1)));}
    private void clearRaisedMango(){upMangoLevel=0;lastMangoEvent="NONE";mangoCursors.clear();}
    private int requiredMango(int seat){int base=rules.mangoScore();if(upMangoLevel<=0)return base;long total=0;int cursor=mangoCursors.getOrDefault(seat,0),end=rules.everyHandMango()?upMangoLevel:upMangoLevel-1;for(int level=cursor;level<end;level++){int n=Math.min(level+1,rules.mangoRaise());total=Math.addExact(total,rules.flipMode()==CxRules.FlipMode.LADDER?Math.multiplyExact((long)base,n+1):Math.multiplyExact((long)base,1L<<n));}if(rules.everyHandMango()&&!playedSeats.contains(seat))total=Math.addExact(total,base);if(total>Integer.MAX_VALUE)throw new IllegalStateException("CX mango exceeds integer range");return(int)total;}
    private void completeSettlement(Map<Long,Long>delta){delta.forEach((playerId,value)->{int seat=players.entrySet().stream().filter(e->e.getValue().equals(playerId)).findFirst().orElseThrow().getKey();scores.put(seat,scores.get(seat)+value);if(value>0)winCounts.merge(seat,1,Integer::sum);else if(value<0)loseCounts.merge(seat,1,Integer::sum);});lastDelta=Map.copyOf(delta);long now=time.epochMillis();players.keySet().forEach(seat->{if(!canFundNextRound(seat))seatRetentionDeadlineEpochMillis.putIfAbsent(seat,Math.addExact(now,Duration.ofSeconds(rules.seatRetentionSeconds()).toMillis()));else seatRetentionDeadlineEpochMillis.remove(seat);});boolean terminal=rules.endMode()==CxRules.EndMode.ROUND_COUNT?round>=rules.endValue():now>=roomEndsAtEpochMillis;if(terminal)openTerminalRetention();else{phase=Phase.ROUND_SETTLEMENT;currentSeat=-1;operationDeadline=OperationDeadline.none();roundSettlementTriggerSeat=fundedSeatCount()>=rules.startPlayers()?players.keySet().stream().filter(this::canFundNextRound).min(Integer::compareTo).orElse(-1):-1;roundSettlementDeadlineEpochMillis=roundSettlementTriggerSeat<0?0:Math.addExact(now,2_000L);}splitDeadlineEpochMillis.clear();stateVersion++;LOG.info("[CX] settlement completed roomId={} roundNo={} endMode={} endValue={} stateVersion={} phase={} nextRoundDeadlineEpochMillis={} nextRoundTriggerSeat={} depletedSeats={} terminalDeadlineEpochMillis={} mangoPool={} playerCount={} playerDelta={} seatScores={} seatCarryScores={}",roomId,round,rules.endMode(),rules.endValue(),stateVersion,phase,roundSettlementDeadlineEpochMillis,roundSettlementTriggerSeat,seatRetentionDeadlineEpochMillis.keySet(),terminalDeadlineEpochMillis,mangoPool,players.size(),lastDelta,scores,carryScores);}
    /** A seat is funded only when it can pay both the base and its next authoritative mango. */
    private boolean canFundNextRound(int seat){return scores.getOrDefault(seat,0L)>Math.addExact(1L,requiredMango(seat));}
    private long fundedSeatCount(){return players.keySet().stream().filter(this::canFundNextRound).count();}
    private void openOperationWindow(){if(phase==Phase.SPLITTING){operationDeadline=OperationDeadline.none();return;}int seat=currentSeat;if(seat<0){operationDeadline=OperationDeadline.none();return;}operationDeadline=OperationDeadline.open("cx:"+roomId+":"+(stateVersion+1)+":"+phase.name()+":"+seat,seat,Duration.ofSeconds(rules.operationSeconds()),time);}
    private int drawCard(){if(deckCursor>=deck.size())throw new IllegalStateException("CX deck exhausted");return deck.get(deckCursor++);}
    /**
     * Advances within one operation phase without inferring wraparound from the
     * current active-seat list. DROP and three-flower can remove the acting seat,
     * so comparing the next seat with the first active seat would end the phase early.
     */
    private void advanceOr(int actedSeat,Phase next){
        phaseActedSeats.add(actedSeat);
        List<Integer> active=activeSeats();
        List<Integer> remaining=active.stream().filter(s->!phaseActedSeats.contains(s)).toList();
        if(remaining.isEmpty()){
            phase=next;
            phaseActedSeats.clear();
            currentSeat=active.isEmpty()?-1:active.get(0);
            return;
        }
        currentSeat=remaining.stream().filter(s->s>actedSeat).findFirst().orElse(remaining.get(0));
    }
    private List<Integer>activeSeats(){return roundSeats.stream().filter(s->!dropped.contains(s)).sorted().toList();}
    private void requireTurn(int seat,Phase expected){requireSeat(seat);if(phase!=expected||currentSeat!=seat)throw new IllegalStateException("not CX turn");}
    private void requireSeat(int seat){if(!players.containsKey(seat))throw new IllegalArgumentException("unknown CX seat");}
    private void mutating(String op){if(op==null||op.isBlank()||operations.contains(op)||phase==Phase.FINISHED)throw new IllegalStateException("invalid/duplicate CX operation");}
    private void changed(String op,String type,int seat){operations.add(op);events.add(Map.of("operationId",op,"type",type,"seat",seat,"stateVersion",stateVersion+1));stateVersion++;LOG.info("[CX] state changed roomId={} playerId={} seatId={} operationId={} action={} phase={} stateVersion={}",roomId,players.get(seat),seat,op,type,phase,stateVersion);}
    private static boolean sameCards(List<Integer>a,List<Integer>b){return a!=null&&b!=null&&a.size()==b.size()&&new HashSet<>(a).equals(new HashSet<>(b));} private static long longValue(Integer v){return v==null?0:v.longValue();}
    public synchronized GameCommandResult execute(GameCommandRequest r){
        if(r.roomId()!=roomId||!r.playVersion().equals(CxRules.VERSION))throw new IllegalArgumentException("CX identity mismatch");
        Map<String,Object>b=r.body().asMap();long playerId=Long.parseLong(r.authenticatedUserId());
        switch(r.msgId()){
            case"poker.chengdu2999.state_req"->{return new GameCommandResult("poker.chengdu2999.state_rsp",r.requestId(),viewFor(playerId)).withAuthorityMetadata(stateVersion,Math.max(1,stateVersion));}
case"poker.chengdu2999.sit_req"->sit(playerId,requiredLong(b,"carryScore"),r.requestId());
case"poker.chengdu2999.ready_req"->legacyReady(playerId,r.requestId());
            case"poker.chengdu2999.continue_req"->{
                mutating(r.requestId());
                requireAuthenticatedSeat(r,playerId);
                if(phase!=Phase.ROUND_SETTLEMENT)throw new IllegalStateException("[CX] continue rejected");
                LOG.info("[CX] next round accepted roomId={} playerId={} seatId={} operationId={} roundNo={} stateVersion={}",roomId,playerId,r.seatId(),r.requestId(),round,stateVersion);
                changed(r.requestId(),"continue",r.seatId());
                startRound();
            }
            case"poker.chengdu2999.restart_req"->restart(r.seatId(),playerId,requiredLong(b,"carryScore"),r.requestId());
            case"poker.chengdu2999.rebuy_req"->rebuy(r.seatId(),playerId,requiredLong(b,"carryScore"),r.requestId());
            case"poker.chengdu2999.stand_req"->stand(r.seatId(),playerId,r.requestId());
            case"poker.chengdu2999.preset_req"->{requireAuthenticatedSeat(r,playerId);presetBet(r.seatId(),number(b,"betType",0),r.requestId());}
            case"poker.chengdu2999.bet_req"->{if(requireLivePlayerOperation(r,playerId))bet(r.seatId(),BetAction.valueOf(String.valueOf(b.get("action"))),number(b,"amount",0),r.requestId());}
            case"poker.chengdu2999.add_card_req"->{if(requireLivePlayerOperation(r,playerId))addCard(r.seatId(),r.requestId());}
            case"poker.chengdu2999.split_req"->{requireAuthenticatedSeat(r,playerId);split(r.seatId(),intList(b.get("cards")),r.requestId());}
            case"poker.chengdu2999.delay_split_req"->{requireAuthenticatedSeat(r,playerId);delaySplit(r.seatId(),r.requestId());}
            case"poker.chengdu2999.timeout_req"->{requireAuthenticatedSeat(r,playerId);timeout(r.seatId(),r.requestId());}
            default->throw new IllegalArgumentException("unsupported CX command");
        }
        replayEvents.add(Map.of("version",stateVersion,"after",authoritativeState()));
        return new GameCommandResult(r.msgId().replace("_req","_rsp"),r.requestId(),viewFor(playerId)).withAuthorityMetadata(stateVersion,stateVersion);
    }
    private boolean requireLivePlayerOperation(GameCommandRequest request,long playerId){Long seatedPlayer=players.get(request.seatId());if(seatedPlayer==null||seatedPlayer!=playerId)throw new IllegalArgumentException("[CX] authenticated seat mismatch");if(!operationDeadline.open()||operationDeadline.seatId()!=request.seatId())throw new IllegalStateException("[CX] operation unavailable");if(!operationDeadline.expired(time))return true;LOG.info("[CX] deadline linearized as timeout roomId={} playerId={} seatId={} operationId={} stateVersion={} deadlineOperationId={}",roomId,playerId,request.seatId(),request.requestId(),stateVersion,operationDeadline.operationId());timeout(request.seatId(),request.requestId());return false;}
    private void requireAuthenticatedSeat(GameCommandRequest request,long playerId){Long seatedPlayer=players.get(request.seatId());if(seatedPlayer==null||seatedPlayer!=playerId)throw new IllegalArgumentException("[CX] authenticated seat mismatch");}
    public synchronized Map<String,Object>viewFor(long viewer){Map<Integer,List<Integer>>visible=new LinkedHashMap<>();hands.forEach((s,c)->{boolean reveal=players.get(s)==viewer||phase==Phase.REVEAL||phase==Phase.ROUND_SETTLEMENT||phase==Phase.FINISHED;List<Integer>cards=new ArrayList<>(c.size());for(int ignored:c)cards.add(reveal?ignored:0);visible.put(s,List.copyOf(cards));});Map<String,Object>m=new LinkedHashMap<>(authoritativeState());m.remove("carryBilling");m.remove("sceneProgress");m.remove("seatRandomSeed");m.remove("seatRandomSequence");m.remove("seatJoinOrder");int viewerSeat=players.entrySet().stream().filter(e->e.getValue()==viewer).map(Map.Entry::getKey).findFirst().orElse(-1);m.put("viewerRole",viewerSeat>=0?"SEATED":"SPECTATOR");m.put("viewerSeat",viewerSeat);m.put("hands",visible);m.put("allowedBetActions",allowedBetActions(viewerSeat));m.put("followAmount",viewerSeat<0?0:Math.max(0,betTarget-bets.getOrDefault(viewerSeat,0)));m.put("minimumRaiseTarget",viewerSeat<0?0:minimumRaiseTarget());m.put("quickRaiseTargets",quickRaiseTargets(viewerSeat));m.put("viewerAvailableScore",viewerSeat<0?0:availableScore(viewerSeat));return Map.copyOf(m);}

    @Override public synchronized CommonRoomPublicView commonRoomPublicView(long viewerPlayerId) {
        int localSeat = -1;
        var seats = new ArrayList<CommonRoomPublicSeat>(rules.maxPlayers());
        for (int seat = 0; seat < rules.maxPlayers(); seat++) {
            Long playerId = players.get(seat);
            if (playerId == null) {
                seats.add(new CommonRoomPublicSeat(seat, false, null, null, null, null));
            } else {
                if (playerId == viewerPlayerId) localSeat = seat;
                Long score = Objects.requireNonNull(scores.get(seat), "occupied CX seat score missing");
                seats.add(new CommonRoomPublicSeat(seat, true, playerId, null, null, score));
            }
        }
        return new CommonRoomPublicView(rules.maxPlayers(), localSeat, seats);
    }
    private List<String>allowedBetActions(int seat){if(seat<0||phase!=Phase.BETTING||currentSeat!=seat)return List.of();List<String>actions=new ArrayList<>();int previous=bets.getOrDefault(seat,0);long available=availableScore(seat);boolean firstFollowRequired=rules.eachPlayerMustFollow()&&hands.get(seat).size()==2&&previous==0;if(!firstFollowRequired)actions.add(BetAction.DROP.name());if(Math.max(0,betTarget-previous)<=available)actions.add(BetAction.FOLLOW.name());if(!firstFollowRequired&&mayRest(seat))actions.add(BetAction.REST.name());if((long)minimumRaiseTarget()-previous<=available)actions.add(BetAction.RAISE.name());if(available>0)actions.add(BetAction.ALL_IN.name());return List.copyOf(actions);}
    private int minimumRaiseTarget(){boolean raised=lastBetActions.values().stream().anyMatch(a->a==BetAction.RAISE||a==BetAction.ALL_IN);return raised?Math.multiplyExact(Math.max(1,betTarget),2):Math.max(rules.openingBet(),betTarget);}
    private List<Integer>quickRaiseTargets(int seat){if(seat<0||phase!=Phase.BETTING||currentSeat!=seat)return List.of();long mangoTotal=roundSeats.stream().mapToLong(s->mangos.getOrDefault(s,rules.mangoScore())).sum()+mangoPool;long base=Math.max(Math.max(rules.openingBet(),betTarget),mangoTotal);boolean rest=mayRest(seat);List<Integer>targets=new ArrayList<>(3);for(int exponent=1;exponent<=3;exponent++){long value=Math.multiplyExact(base,1L<<exponent);if(rest)value/=2;if(value>Integer.MAX_VALUE)throw new IllegalStateException("CX quick raise exceeds integer range");targets.add((int)value);}return List.copyOf(targets);}
    public synchronized Map<String,Object>authoritativeState(){
        // Win/loss totals are durable authoritative room data used by final settlement and reconnect.
        Map<String,Object>m=new LinkedHashMap<>();m.put("schemaVersion",10);m.put("carryBilling",carryBillingState());m.put("sceneProgress",sceneProgressState());m.put("gameCode",CxRules.GAME_CODE);m.put("playVersion",CxRules.VERSION);m.put("roomId",roomId);m.put("ownerId",ownerId);m.put("seed",seed);m.put("seatRandomSeed",seatRandomSeed);m.put("seatRandomSequence",seatRandomSequence);m.put("rules",rules.toMap());m.put("phase",phase.name());m.put("round",round);m.put("bankerSeat",bankerSeat);m.put("stateVersion",stateVersion);m.put("roomStartedAtEpochMillis",roomStartedAtEpochMillis);m.put("roomEndsAtEpochMillis",roomEndsAtEpochMillis);m.put("terminalDeadlineEpochMillis",terminalDeadlineEpochMillis);m.put("roundSettlementDeadlineEpochMillis",roundSettlementDeadlineEpochMillis);m.put("roundSettlementTriggerSeat",roundSettlementTriggerSeat);m.put("seatRetentionDeadlineEpochMillis",Map.copyOf(seatRetentionDeadlineEpochMillis));m.put("restartCarryScores",Map.copyOf(restartCarryScores));m.put("mangoPool",mangoPool);m.put("upMangoLevel",upMangoLevel);m.put("lastMangoEvent",lastMangoEvent);m.put("mangoCursors",Map.copyOf(mangoCursors));m.put("winCounts",Map.copyOf(winCounts));m.put("loseCounts",Map.copyOf(loseCounts));m.put("playedSeats",List.copyOf(playedSeats));m.put("roundSeats",List.copyOf(roundSeats));m.put("requiredMangos",players.keySet().stream().collect(java.util.stream.Collectors.toMap(s->s,this::requiredMango,(a,b)->a,LinkedHashMap::new)));m.put("betTarget",betTarget);m.put("lastBetActions",lastBetActions.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().name(),(a,b)->a,LinkedHashMap::new)));m.put("allInSeats",List.copyOf(allInSeats));m.put("operationDeadline",operationDeadline.toMap());m.put("splitDeadlineEpochMillis",Map.copyOf(splitDeadlineEpochMillis));m.put("timeoutCounts",Map.copyOf(timeoutCounts));m.put("splitDelayCounts",Map.copyOf(splitDelayCounts));m.put("hostingSeats",List.copyOf(hostingSeats));m.put("players",Map.copyOf(players));m.put("seatJoinOrder",List.copyOf(players.keySet()));m.put("readySeats",List.copyOf(ready));m.put("hands",Map.copyOf(hands));m.put("committed",Map.copyOf(committed));m.put("droppedSeats",List.copyOf(dropped));m.put("threeFlowerSeats",List.copyOf(threeFlowerSeats));m.put("splitSeats",List.copyOf(split));m.put("carryScores",Map.copyOf(carryScores));m.put("scores",Map.copyOf(scores));m.put("bases",Map.copyOf(bases));m.put("mangos",Map.copyOf(mangos));m.put("bets",Map.copyOf(bets));m.put("presetBetTypes",Map.copyOf(presetBetTypes));m.put("operations",List.copyOf(operations));m.put("phaseActedSeats",List.copyOf(phaseActedSeats));m.put("deck",List.copyOf(deck));m.put("deckCursor",deckCursor);m.put("currentSeat",currentSeat);m.put("lastDelta",Map.copyOf(lastDelta));return Map.copyOf(m);
    }
    public long stateVersion(){return stateVersion;}public OperationDeadline operationDeadline(){return operationDeadline;}public OperationDeadlineArbiter deadlineArbiter(){return arbiter;}public List<String>invariantViolations(){List<String>v=new ArrayList<>();if(players.size()>rules.maxPlayers())v.add("too many players");if(scores.size()!=players.size())v.add("score/player mismatch");if(!carryScores.keySet().equals(players.keySet()))v.add("carry-score/player mismatch");if(!ready.equals(players.keySet()))v.add("seated/ready mismatch");if(deckCursor<0||deckCursor>deck.size())v.add("invalid deck cursor");if(phase==Phase.FINISHED&&operationDeadline.open())v.add("finished room has open deadline");if(operationDeadline.open()&&!players.containsKey(operationDeadline.seatId()))v.add("deadline seat missing");if(phase!=Phase.SPLITTING&&!splitDeadlineEpochMillis.isEmpty())v.add("split deadline outside split phase");if(!players.keySet().containsAll(splitDeadlineEpochMillis.keySet()))v.add("split deadline seat missing");return List.copyOf(v);}public SettlementPayload settlement(int roundNo,String version){if(roundNo!=round||!version.equals(CxRules.VERSION)||lastDelta.isEmpty())throw new IllegalStateException("CX settlement unavailable");return new SettlementPayload(roomId,roundNo,version,lastDelta);}public List<Object>recordedEvents(){return List.copyOf(replayEvents);}
    public static CxSession restore(Map<String,Object>s){if(!CxRules.GAME_CODE.equals(String.valueOf(s.get("gameCode")))||!CxRules.VERSION.equals(String.valueOf(s.get("playVersion"))))throw new IllegalArgumentException("[CX] restore identity mismatch");CxSession x=new CxSession(lng(s.get("roomId")),lng(s.get("ownerId")),lng(s.get("seed")),CxRules.restoreFrom(map(s.get("rules"))));x.players.clear();copyLongMap(s.get("players"),x.players);restoreSeatJoinOrder(s,x);x.seatRandomSeed=lngOr(s.get("seatRandomSeed"),seatDrawSeed(x.seed,0L));x.seatRandomSequence=lngOr(s.get("seatRandomSequence"),x.players.size());x.scores.clear();copyLongMap(s.get("scores"),x.scores);x.carryScores.clear();copyLongMap(s.get("carryScores"),x.carryScores);x.players.keySet().forEach(seat->x.carryScores.putIfAbsent(seat,0L));copyIntMap(s.get("winCounts"),x.winCounts);copyIntMap(s.get("loseCounts"),x.loseCounts);x.players.keySet().forEach(seat->{x.winCounts.putIfAbsent(seat,0);x.loseCounts.putIfAbsent(seat,0);});copyIntMap(s.get("committed"),x.committed);copyIntMap(s.get("bases"),x.bases);copyIntMap(s.get("mangos"),x.mangos);copyIntMap(s.get("bets"),x.bets);copyIntMap(s.get("presetBetTypes"),x.presetBetTypes);copyIntMap(s.get("mangoCursors"),x.mangoCursors);copyIntMap(s.get("timeoutCounts"),x.timeoutCounts);copyIntMap(s.get("splitDelayCounts"),x.splitDelayCounts);copyLongMap(s.get("splitDeadlineEpochMillis"),x.splitDeadlineEpochMillis);copyLongMap(s.get("seatRetentionDeadlineEpochMillis"),x.seatRetentionDeadlineEpochMillis);map(s.get("lastBetActions")).forEach((k,v)->x.lastBetActions.put(Integer.parseInt(k),BetAction.valueOf(String.valueOf(v))));copyHands(s.get("hands"),x.hands);x.ready.addAll(intList(s.get("readySeats")));x.dropped.addAll(intList(s.get("droppedSeats")));x.threeFlowerSeats.addAll(intList(s.get("threeFlowerSeats")));x.split.addAll(intList(s.get("splitSeats")));x.allInSeats.addAll(intList(s.get("allInSeats")));x.hostingSeats.addAll(intList(s.get("hostingSeats")));x.phaseActedSeats.addAll(intList(s.get("phaseActedSeats")));x.playedSeats.addAll(intList(s.get("playedSeats")));x.roundSeats.addAll(intList(s.get("roundSeats")));for(Object o:list(s.get("operations")))x.operations.add(String.valueOf(o));x.deck.addAll(intList(s.get("deck")));x.deckCursor=number(s,"deckCursor",0);x.currentSeat=number(s,"currentSeat",-1);x.bankerSeat=number(s,"bankerSeat",-1);x.betTarget=number(s,"betTarget",0);x.upMangoLevel=number(s,"upMangoLevel",0);x.roundSettlementTriggerSeat=number(s,"roundSettlementTriggerSeat",-1);x.lastMangoEvent=String.valueOf(s.getOrDefault("lastMangoEvent","NONE"));x.round=number(s,"round",0);x.stateVersion=lng(s.get("stateVersion"));x.roomStartedAtEpochMillis=lngOr(s.get("roomStartedAtEpochMillis"),0);x.roomEndsAtEpochMillis=lngOr(s.get("roomEndsAtEpochMillis"),0);x.terminalDeadlineEpochMillis=lngOr(s.get("terminalDeadlineEpochMillis"),0);x.roundSettlementDeadlineEpochMillis=lngOr(s.get("roundSettlementDeadlineEpochMillis"),0);copyLongMap(s.get("restartCarryScores"),x.restartCarryScores);x.mangoPool=lngOr(s.get("mangoPool"),0);x.operationDeadline=OperationDeadline.from(s.get("operationDeadline"));x.phase=Phase.valueOf(String.valueOf(s.get("phase")));copyPlayerDelta(s.get("lastDelta"),x.lastDelta=new LinkedHashMap<>());if(x.phase==Phase.ROUND_SETTLEMENT&&x.roundSettlementDeadlineEpochMillis<=0&&x.fundedSeatCount()>=x.rules.startPlayers()){x.roundSettlementTriggerSeat=x.players.keySet().stream().filter(seat->!x.seatRetentionDeadlineEpochMillis.containsKey(seat)).min(Integer::compareTo).orElse(-1);x.roundSettlementDeadlineEpochMillis=Math.addExact(x.time.epochMillis(),2_000L);LOG.info("[CX] legacy settlement restored with auto-next deadline roomId={} roundNo={} stateVersion={} deadlineEpochMillis={} triggerSeat={}",x.roomId,x.round,x.stateVersion,x.roundSettlementDeadlineEpochMillis,x.roundSettlementTriggerSeat);}x.restoreCarryBilling(s.get("carryBilling"));if(s.containsKey("sceneProgress")&&s.get("sceneProgress")==null)throw new IllegalArgumentException("sceneProgress null");x.restoreSceneProgress(s.get("sceneProgress"));return x;}
    public static StatePayload replay(StatePayload base,List<Object>ordered){Map<String,Object>state=new LinkedHashMap<>(base.asMap());long version=lng(state.get("stateVersion"));for(Object raw:ordered){Map<String,Object>event=map(raw);long next=lng(event.get("version"));if(next<=version)throw new IllegalArgumentException("[CX] replay version must increase");state=new LinkedHashMap<>(map(event.get("after")));if(lng(state.get("stateVersion"))!=next)throw new IllegalArgumentException("[CX] replay event/state mismatch");version=next;}return StatePayload.copyOf(state);}
    /** The linked player map is the sole live ordering source; the list is its durable snapshot projection. */
    private static void restoreSeatJoinOrder(Map<String,Object> snapshot,CxSession session){
        Object raw=snapshot.get("seatJoinOrder");
        if(raw==null){
            if(session.players.size()>1)throw new IllegalArgumentException("[CX] legacy multi-player snapshot missing seatJoinOrder roomId="+session.roomId);
            return;
        }
        if(!(raw instanceof List<?>))throw new IllegalArgumentException("[CX] invalid seatJoinOrder roomId="+session.roomId);
        List<Integer> order=intList(raw);
        if(order.size()!=session.players.size()||new HashSet<>(order).size()!=order.size()||!session.players.keySet().containsAll(order))throw new IllegalArgumentException("[CX] seatJoinOrder does not match players roomId="+session.roomId);
        Map<Integer,Long> bySeat=new LinkedHashMap<>(session.players);
        session.players.clear();
        for(int seat:order)session.players.put(seat,bySeat.get(seat));
    }
    private static int number(Map<String,Object>m,String k,int d){Object v=m.get(k);return v instanceof Number n?n.intValue():d;}private static long requiredLong(Map<String,Object>m,String k){Object v=m.get(k);if(!(v instanceof Byte||v instanceof Short||v instanceof Integer||v instanceof Long))throw new IllegalArgumentException("[CX] missing or invalid "+k);return ((Number)v).longValue();}private static long lng(Object v){return v instanceof Number n?n.longValue():Long.parseLong(String.valueOf(v));}private static long lngOr(Object v,long d){return v==null?d:lng(v);}private static List<?>list(Object v){return v instanceof Collection<?>c?List.copyOf(c):List.of();}private static List<Integer>intList(Object v){return list(v).stream().map(x->((Number)x).intValue()).toList();}@SuppressWarnings("unchecked")private static Map<String,Object>map(Object v){Map<String,Object>m=new LinkedHashMap<>();if(v instanceof Map<?,?>raw)raw.forEach((k,n)->m.put(String.valueOf(k),n));return m;}private static void copyLongMap(Object raw,Map<Integer,Long>out){map(raw).forEach((k,v)->out.put(Integer.parseInt(k),lng(v)));}private static void copyIntMap(Object raw,Map<Integer,Integer>out){map(raw).forEach((k,v)->out.put(Integer.parseInt(k),((Number)v).intValue()));}private static void copyHands(Object raw,Map<Integer,List<Integer>>out){map(raw).forEach((k,v)->out.put(Integer.parseInt(k),List.copyOf(intList(v))));}private static void copyPlayerDelta(Object raw,Map<Long,Long>out){map(raw).forEach((k,v)->out.put(Long.parseLong(k),lng(v)));}

    private Integer validateSitCarry(long playerId,long amount,String op){
        mutating(op);
        if(amount<0||amount>1_000_000_000L)throw sitRejected("CX_INVALID_CARRY_SCORE",-1,playerId,op,true);
        Integer occupied=players.entrySet().stream().filter(e->e.getValue()==playerId).map(Map.Entry::getKey).findFirst().orElse(null);
        if(occupied!=null){long deadline=seatRetentionDeadlineEpochMillis.getOrDefault(occupied,0L);if(deadline<=0||time.epochMillis()>=deadline||amount<=0)throw sitRejected("CX_ALREADY_SEATED",occupied,playerId,op,false);}
        else if((phase!=Phase.WAITING&&phase!=Phase.ROUND_SETTLEMENT)||players.size()>=rules.maxPlayers())throw sitRejected("CX_SIT_UNAVAILABLE",-1,playerId,op,false);
        new Proposal(op,roomId,playerId,occupied==null?-1:occupied,amount,occupied==null?Kind.INITIAL:Kind.CONFIRM);
        return occupied;
    }
    private void validateRebuyCarry(int seat,long playerId,long amount,String op){
        mutating(op);if(players.get(seat)==null||players.get(seat)!=playerId)throw new IllegalArgumentException("[CX] authenticated seat mismatch");
        long deadline=seatRetentionDeadlineEpochMillis.getOrDefault(seat,0L),minimum=Math.addExact(1L,requiredMango(seat));
        if(phase!=Phase.ROUND_SETTLEMENT||deadline<=0||time.epochMillis()>=deadline||amount<=minimum||amount>1_000_000_000L)throw new IllegalStateException("[CX] rebuy unavailable");
        new Proposal(op,roomId,playerId,seat,amount,Kind.REBUY);
    }
    @Override public synchronized Optional<Proposal> proposeCarry(GameCommandRequest request){
        Kind kind;String msg=request.msgId();
        if(!Set.of("poker.chengdu2999.sit_req","poker.chengdu2999.rebuy_req","poker.chengdu2999.restart_req").contains(msg))return Optional.empty();
        if(request.roomId()!=roomId||!CxRules.VERSION.equals(request.playVersion()))throw new IllegalArgumentException("CX identity mismatch");
        long actor=Long.parseLong(request.authenticatedUserId()),amount=requiredLong(request.body().asMap(),"carryScore");int seat=request.seatId();
        if(msg.endsWith(".sit_req")){Integer occupied=validateSitCarry(actor,amount,request.requestId());seat=occupied==null?-1:occupied;kind=occupied==null?Kind.INITIAL:Kind.CONFIRM;}
        else if(msg.endsWith(".rebuy_req")){validateRebuyCarry(seat,actor,amount,request.requestId());kind=Kind.REBUY;}
        else {terminalMutation(seat,actor,amount,request.requestId());validateSceneRequest(request.requestId());kind=Kind.RESTART;}
        return Optional.of(new Proposal(request.requestId(),roomId,actor,seat,amount,kind));
    }
    private CarryAttempt requestCarry(Kind kind,int seat,long actor,long amount,String op){
        if(seat<0)throw new IllegalStateException("carry accepted without resolved seat");
        CarryAttempt attempt=new CarryAttempt(new Proposal(op,roomId,actor,seat,amount,kind));
        if(carryAttempts.containsKey(attempt.id))throw new IllegalStateException("duplicate carry attempt");
        terminateUnusedCarry(pendingCarryBySeat.remove(seat));
        carryAttempts.put(attempt.id,attempt);pendingCarryBySeat.put(seat,attempt.id);emitCarry(attempt,Stage.REQUESTED);return attempt;
    }
    private void applyCarry(CarryAttempt attempt){
        if(attempt==null||attempt.applied||attempt.terminated)throw new IllegalStateException("invalid carry application");
        int seat=attempt.proposal.seatId();String previous=appliedCarryBySeat.get(seat);if(!attempt.id.equals(previous))terminateUnusedCarry(previous);
        attempt.applied=true;pendingCarryBySeat.remove(seat,attempt.id);appliedCarryBySeat.put(seat,attempt.id);emitCarry(attempt,Stage.APPLIED);
    }
    private void useCarry(int seat){String id=appliedCarryBySeat.get(seat);if(id==null)return;CarryAttempt attempt=carryAttempts.get(id);if(attempt.applied&&!attempt.used&&!attempt.terminated){attempt.used=true;emitCarry(attempt,Stage.USED);}}
    private void terminateUnusedCarry(String id){if(id==null)return;CarryAttempt attempt=carryAttempts.get(id);if(attempt==null)throw new IllegalStateException("unknown carry attempt reference");if(!attempt.used&&!attempt.terminated){attempt.terminated=true;emitCarry(attempt,Stage.TERMINATED_UNUSED);}}
    private void terminateAllUnusedCarry(){for(String id:List.copyOf(pendingCarryBySeat.values()))terminateUnusedCarry(id);for(String id:List.copyOf(appliedCarryBySeat.values()))terminateUnusedCarry(id);pendingCarryBySeat.clear();appliedCarryBySeat.clear();}
    private void emitCarry(CarryAttempt attempt,Stage stage){Proposal p=attempt.proposal;carryEffects.add(Effect.create(p.originalRequestId(),roomId,p.authenticatedActorId(),p.seatId(),p.carryScore(),p.kind(),stage,++carryOrdinal,stateVersion));}
    @Override public synchronized Batch pendingCarryEffects(){return new Batch(roomId,carryOrdinal,List.copyOf(carryEffects));}
    @Override public synchronized void acknowledgeCarryEffects(long throughOrdinal,String batchHash){
        if(throughOrdinal==carryAckOrdinal&&Objects.equals(batchHash,carryAckHash))return;
        if(carryEffects.isEmpty()&&throughOrdinal==carryAckOrdinal&&throughOrdinal==carryOrdinal&&pendingCarryEffects().hash().equals(batchHash))return;
        List<Effect> prefix=carryEffects.stream().filter(e->e.ordinal()<=throughOrdinal).toList();
        if(throughOrdinal<=carryAckOrdinal||throughOrdinal>carryOrdinal||prefix.isEmpty()||!new Batch(roomId,throughOrdinal,prefix).hash().equals(batchHash))throw new IllegalArgumentException("carry ack prefix mismatch");
        carryEffects.removeIf(e->e.ordinal()<=throughOrdinal);carryAckOrdinal=throughOrdinal;carryAckHash=batchHash;
    }
    private Map<String,Object> carryBillingState(){
        List<Map<String,Object>> attempts=new ArrayList<>();
        for(CarryAttempt a:carryAttempts.values()){Proposal p=a.proposal;Map<String,Object> row=new LinkedHashMap<>();row.put("attemptId",a.id);row.put("originalRequestId",p.originalRequestId());row.put("roomId",p.roomId());row.put("actorId",p.authenticatedActorId());row.put("seatId",p.seatId());row.put("carryScore",p.carryScore());row.put("kind",p.kind().name());row.put("applied",a.applied);row.put("used",a.used);row.put("terminated",a.terminated);attempts.add(Map.copyOf(row));}
        List<Map<String,Object>> effects=new ArrayList<>();for(Effect e:carryEffects){Map<String,Object> row=new LinkedHashMap<>();row.put("effectId",e.effectId());row.put("attemptId",e.attemptId());row.put("originalRequestId",e.originalRequestId());row.put("roomId",e.roomId());row.put("actorId",e.actorId());row.put("seatId",e.seatId());row.put("carryScore",e.carryScore());row.put("kind",e.kind().name());row.put("stage",e.stage().name());row.put("ordinal",e.ordinal());row.put("stateVersion",e.stateVersion());effects.add(Map.copyOf(row));}
        return Map.of("schemaVersion",1,"throughOrdinal",carryOrdinal,"ackThroughOrdinal",carryAckOrdinal,"ackBatchHash",carryAckHash,"attempts",List.copyOf(attempts),"pendingBySeat",Map.copyOf(pendingCarryBySeat),"appliedBySeat",Map.copyOf(appliedCarryBySeat),"effects",List.copyOf(effects));
    }
    private void restoreCarryBilling(Object raw){
        if(raw==null)return;Map<String,Object> state=map(raw);if(number(state,"schemaVersion",0)!=1)throw new IllegalArgumentException("unknown carry proof schema");
        carryOrdinal=lng(state.get("throughOrdinal"));carryAckOrdinal=lng(state.get("ackThroughOrdinal"));carryAckHash=String.valueOf(state.get("ackBatchHash"));if(carryAckOrdinal<0||carryOrdinal<carryAckOrdinal)throw new IllegalArgumentException("invalid carry proof cursor");
        for(Object value:list(state.get("attempts"))){Map<String,Object> row=map(value);Proposal proposal=new Proposal(String.valueOf(row.get("originalRequestId")),lng(row.get("roomId")),lng(row.get("actorId")),number(row,"seatId",-1),lng(row.get("carryScore")),Kind.valueOf(String.valueOf(row.get("kind"))));CarryAttempt a=new CarryAttempt(proposal);
            if(proposal.roomId()!=roomId||proposal.seatId()<0||!a.id.equals(row.get("attemptId"))||carryAttempts.putIfAbsent(a.id,a)!=null)throw new IllegalArgumentException("invalid carry attempt proof");a.applied=Boolean.TRUE.equals(row.get("applied"));a.used=Boolean.TRUE.equals(row.get("used"));a.terminated=Boolean.TRUE.equals(row.get("terminated"));if(a.used&&(!a.applied||a.terminated))throw new IllegalArgumentException("invalid carry attempt stages");}
        restoreCarryIndex(state.get("pendingBySeat"),pendingCarryBySeat,false);restoreCarryIndex(state.get("appliedBySeat"),appliedCarryBySeat,true);
        for(Object value:list(state.get("effects"))){Map<String,Object> row=map(value);Effect e=new Effect(String.valueOf(row.get("effectId")),String.valueOf(row.get("attemptId")),String.valueOf(row.get("originalRequestId")),lng(row.get("roomId")),lng(row.get("actorId")),number(row,"seatId",-1),lng(row.get("carryScore")),Kind.valueOf(String.valueOf(row.get("kind"))),Stage.valueOf(String.valueOf(row.get("stage"))),lng(row.get("ordinal")),lng(row.get("stateVersion")));
            CarryAttempt a=carryAttempts.get(e.attemptId());if(a==null||e.roomId()!=roomId||e.seatId()<0||e.ordinal()<=carryAckOrdinal||e.seatId()!=a.proposal.seatId()||e.carryScore()!=a.proposal.carryScore())throw new IllegalArgumentException("invalid pending carry effect");carryEffects.add(e);}
        new Batch(roomId,carryOrdinal,carryEffects);if(carryEffects.isEmpty()&&carryOrdinal!=carryAckOrdinal)throw new IllegalArgumentException("missing pending carry proof");
    }
    private void restoreCarryIndex(Object raw,Map<Integer,String> target,boolean applied){for(var entry:map(raw).entrySet()){int seat=Integer.parseInt(String.valueOf(entry.getKey()));String id=String.valueOf(entry.getValue());CarryAttempt a=carryAttempts.get(id);if(a==null||a.proposal.seatId()!=seat||a.applied!=applied||a.terminated)throw new IllegalArgumentException("invalid carry seat reference");target.put(seat,id);}}

    private void validateSceneRequest(String request){
        if(sceneAttempts.containsKey(request))throw new IllegalStateException("duplicate scene request");
        if(sceneAttempts.size()>=SCENE_ATTEMPT_LIMIT)throw new IllegalStateException("scene progress capacity reached");
        Math.addExact(sceneOrdinal,1L);
    }
    private void acceptSceneRequest(String request,long actor,int seat){
        abortSceneRequest(pendingSceneBySeat.remove(seat),"SUPERSEDED");
        SceneAttempt attempt=new SceneAttempt(request,actor,seat,Math.addExact(sceneOrdinal,1L),stateVersion);
        sceneAttempts.put(request,attempt);pendingSceneBySeat.put(seat,request);
    }
    private void abortSceneRequest(String request,String reason){
        if(request==null)return;SceneAttempt a=sceneAttempts.get(request);if(a==null)throw new IllegalStateException("unknown scene request");
        if(a.status==SceneStatus.PENDING||a.status==SceneStatus.APPLIED){a.status=SceneStatus.ABORTED;a.abortReason=reason;}
    }
    private void abortUnopenedScene(String reason){
        for(String request:pendingSceneBySeat.values())abortSceneRequest(request,reason);
        for(String request:activeSceneAttempts)abortSceneRequest(request,reason);
        pendingSceneBySeat.clear();activeSceneAttempts.clear();
    }
    private void applyPendingScene(){
        long next=Math.addExact(sceneOrdinal,1L);
        for(var e:pendingSceneBySeat.entrySet()){SceneAttempt a=sceneAttempts.get(e.getValue());if(a==null||a.status!=SceneStatus.PENDING||a.target!=next||a.seat!=e.getKey())throw new IllegalStateException("invalid scene application");}
        sceneOrdinal=next;activeSceneAttempts.clear();
        for(String request:pendingSceneBySeat.values()){SceneAttempt a=sceneAttempts.get(request);a.status=SceneStatus.APPLIED;a.appliedVersion=stateVersion;activeSceneAttempts.add(request);}
        pendingSceneBySeat.clear();
    }
    private void commitSceneOpening(){
        if(round!=1||hands.size()<2||phase==Phase.FINISHED||activeSceneAttempts.isEmpty())return;
        for(int seat:hands.keySet())if(committed.getOrDefault(seat,0)<=0)return;
        for(String request:activeSceneAttempts){SceneAttempt a=sceneAttempts.get(request);if(a.status==SceneStatus.APPLIED&&a.target==sceneOrdinal){a.status=SceneStatus.FIRST_OPENING_COMMITTED;a.openingVersion=stateVersion;}}
    }
    private Map<String,Object> sceneProgressState(){
        List<Map<String,Object>> rows=new ArrayList<>();
        for(SceneAttempt a:sceneAttempts.values()){Map<String,Object> row=new LinkedHashMap<>();row.put("originalRestartRequestId",a.request);row.put("actorId",a.actor);row.put("seatId",a.seat);row.put("targetSceneOrdinal",a.target);row.put("status",a.status.name());row.put("acceptedStateVersion",a.acceptedVersion);if(a.appliedVersion>=0)row.put("appliedStateVersion",a.appliedVersion);if(a.openingVersion>=0)row.put("firstOpeningStateVersion",a.openingVersion);if(!a.abortReason.isEmpty())row.put("abortReason",a.abortReason);rows.add(Map.copyOf(row));}
        return Map.of("schemaVersion",1,"sceneOrdinal",sceneOrdinal,"attempts",List.copyOf(rows),"pendingBySeat",Map.copyOf(pendingSceneBySeat),"activeSceneAttempts",List.copyOf(activeSceneAttempts));
    }
    private void restoreSceneProgress(Object raw){
        if(raw==null)return;Map<String,Object> state=map(raw);if(number(state,"schemaVersion",0)!=1)throw new IllegalArgumentException("unknown scene progress schema");sceneOrdinal=lng(state.get("sceneOrdinal"));if(sceneOrdinal<0)throw new IllegalArgumentException("negative scene ordinal");
        List<?> rows=list(state.get("attempts"));if(rows.size()>SCENE_ATTEMPT_LIMIT)throw new IllegalArgumentException("scene capacity exceeded");
        for(Object value:rows){Map<String,Object> row=map(value);if(!row.containsKey("originalRestartRequestId")||row.get("originalRestartRequestId")==null)throw new IllegalArgumentException("scene request required");String request=String.valueOf(row.get("originalRestartRequestId"));long actor=lng(row.get("actorId")),target=lng(row.get("targetSceneOrdinal")),accepted=lng(row.get("acceptedStateVersion"));int seat=number(row,"seatId",-1);
            if(request.isBlank()||request.length()>128||request.chars().anyMatch(Character::isISOControl)||actor<=0||seat<0||seat>=rules.maxPlayers()||target<=0||accepted<0||accepted>stateVersion)throw new IllegalArgumentException("invalid scene identity");
            SceneAttempt a=new SceneAttempt(request,actor,seat,target,accepted);a.status=SceneStatus.valueOf(String.valueOf(row.get("status")));a.appliedVersion=lngOr(row.get("appliedStateVersion"),-1);a.openingVersion=lngOr(row.get("firstOpeningStateVersion"),-1);a.abortReason=String.valueOf(row.getOrDefault("abortReason",""));
            if(sceneAttempts.putIfAbsent(request,a)!=null)throw new IllegalArgumentException("duplicate scene identity");
            boolean applied=a.appliedVersion>=0,opened=a.openingVersion>=0;
            if((a.status==SceneStatus.PENDING&&(applied||opened||sceneOrdinal==Long.MAX_VALUE||target!=sceneOrdinal+1))||(a.status==SceneStatus.APPLIED&&(!applied||opened||target>sceneOrdinal))||(a.status==SceneStatus.FIRST_OPENING_COMMITTED&&(!applied||!opened||target>sceneOrdinal))||(a.status==SceneStatus.ABORTED&&(opened||a.abortReason.isBlank()))||a.appliedVersion>stateVersion||a.openingVersion>stateVersion||(applied&&a.appliedVersion<accepted)||(opened&&a.openingVersion<a.appliedVersion))throw new IllegalArgumentException("invalid scene stage proof");
        }
        for(var entry:map(state.get("pendingBySeat")).entrySet()){int seat=Integer.parseInt(String.valueOf(entry.getKey()));String request=String.valueOf(entry.getValue());SceneAttempt a=sceneAttempts.get(request);if(a==null||a.status!=SceneStatus.PENDING||a.seat!=seat||!Objects.equals(players.get(seat),a.actor)||!restartCarryScores.containsKey(seat))throw new IllegalArgumentException("invalid scene pending reference");pendingSceneBySeat.put(seat,request);}
        for(Object value:list(state.get("activeSceneAttempts"))){String request=String.valueOf(value);SceneAttempt a=sceneAttempts.get(request);if(a==null||a.target!=sceneOrdinal||(a.status!=SceneStatus.APPLIED&&a.status!=SceneStatus.FIRST_OPENING_COMMITTED)||!activeSceneAttempts.add(request))throw new IllegalArgumentException("invalid active scene reference");}
        for(SceneAttempt a:sceneAttempts.values()){if(a.status==SceneStatus.PENDING&&!Objects.equals(pendingSceneBySeat.get(a.seat),a.request))throw new IllegalArgumentException("unreferenced pending scene");if(a.status==SceneStatus.APPLIED&&!activeSceneAttempts.contains(a.request))throw new IllegalArgumentException("unreferenced applied scene");}
    }
}
