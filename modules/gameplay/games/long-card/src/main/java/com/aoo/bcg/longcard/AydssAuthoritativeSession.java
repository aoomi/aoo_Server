package com.aoo.bcg.longcard;

import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;
import java.util.*;

/** Sole mutable AYDSS authority; client payloads are hints and never state. */
public final class AydssAuthoritativeSession implements AuthoritativeGameSession {
    private final long roomId, ownerId;
    private final RulePayload rules;
    private final Map<Integer,Long> seats = new LinkedHashMap<>();
    private final Set<Integer> ready = new LinkedHashSet<>();
    private final OperationDeadlineArbiter arbiter = new OperationDeadlineArbiter();
    private LongCardState state;
    private long lastSequence;
    private long stateVersion;
    private final List<Object> events = new ArrayList<>();

    public AydssAuthoritativeSession(long roomId,long ownerId,RulePayload rules) {
        if(roomId<=0||ownerId<=0) throw new IllegalArgumentException("invalid AYDSS room");
        this.roomId=roomId;this.ownerId=ownerId;this.rules=rules==null?RulePayload.empty():rules;seats.put(0,ownerId);
    }
    static AydssAuthoritativeSession restore(Map<String,Object> snapshot) {
        if(number(snapshot.get("schemaVersion"),-1)!=1)throw new IllegalArgumentException("unsupported AYDSS snapshot schema");
        long room=longNumber(snapshot.get("roomId")); long owner=longNumber(snapshot.get("ownerId"));
        AydssAuthoritativeSession session=new AydssAuthoritativeSession(room,owner,RulePayload.copyOf(stringMap(snapshot.get("rules"))));
        session.seats.clear(); stringMap(snapshot.get("players")).forEach((seat,id)->session.seats.put(Integer.parseInt(seat),longNumber(id)));
        collection(snapshot.get("ready")).forEach(seat->session.ready.add(number(seat,-1)));
        Object encoded=snapshot.get("state"); if(encoded instanceof Map<?,?>)session.state=decodeState(stringMap(encoded));
        session.lastSequence=longNumber(snapshot.getOrDefault("lastSequence",0));
        session.stateVersion=longNumber(snapshot.getOrDefault("stateVersion",0));
        if(!session.invariantViolations().isEmpty())throw new IllegalArgumentException("invalid AYDSS snapshot: "+session.invariantViolations());
        return session;
    }
    @Override public synchronized GameCommandResult execute(GameCommandRequest request) {
        if(request.sequence()<=lastSequence) throw new IllegalArgumentException("sequence must increase");
        long player=player(request.authenticatedUserId());String action=action(request.body());
        switch(action) {
            case "CAYDSSGetRoomInfo", "get_room_info", "state" -> requireParticipant(player);
            case "join" -> join(request.seatId(),player);
            case "CAYDSSReadyRoom", "ready" -> { own(request.seatId(),player); if(state!=null)throw new IllegalStateException("round started");ready.add(request.seatId()); }
            case "CAYDSSUnReadyRoom", "unready" -> { own(request.seatId(),player); if(state!=null)throw new IllegalStateException("round started");ready.remove(request.seatId()); }
            case "CAYDSSStartGame", "start" -> start(player);
            case "CAYDSSOpCard", "operation" -> operate(request,player);
            default -> throw new IllegalArgumentException("unsupported AYDSS action: "+action);
        }
        lastSequence=request.sequence();if(!action.equals("state")&&!action.equals("get_room_info")&&!action.equals("CAYDSSGetRoomInfo")){stateVersion=Math.addExact(stateVersion,1);events.add(Map.of("version",stateVersion,"after",snapshot()));}
        return new GameCommandResult("longcard.aydss.response",request.requestId(),viewFor(player));
    }
    private void join(int seat,long player){if(state!=null)throw new IllegalStateException("round started");if(seat<0||seat>=4)throw new IllegalArgumentException("invalid seat");if(seats.containsValue(player))return;Long old=seats.putIfAbsent(seat,player);if(old!=null)throw new IllegalStateException("seat occupied");}
    private void start(long player){if(player!=ownerId)throw new SecurityException("owner only");if(state!=null)throw new IllegalStateException("round started");if(seats.size()<2||!ready.containsAll(seats.keySet().stream().filter(s->s!=0).toList()))throw new IllegalStateException("players not ready");List<Integer>deck=new ArrayList<>();for(int copy=0;copy<4;copy++)for(int card=1;card<=20;card++)deck.add(copy*100+card);Collections.shuffle(deck,new Random(roomId));Map<Integer,List<Integer>>hands=new LinkedHashMap<>();for(int seat:seats.keySet()){List<Integer>hand=new ArrayList<>();for(int i=0;i<14;i++)hand.add(deck.remove(0));hands.put(seat,hand);}state=new LongCardState(hands,deck,List.of(),0,LongCardPhase.PLAYING,null,Set.of(),null);}
    private void operate(GameCommandRequest request,long player){own(request.seatId(),player);if(state==null)throw new IllegalStateException("round not started");CommandPayload payload=payload(request.body());int op=number(payload.asMap().get("opType"),-1);Object raw=payload.asMap().get("cardID");if(raw==null)raw=payload.asMap().get("cardId");List<Integer>cards=raw instanceof Collection<?> c?c.stream().map(v->number(v,-1)).toList():raw==null?List.of():List.of(number(raw,-1));LongCardOperation operation=switch(op){case 7->LongCardOperation.DISCARD;case 6->LongCardOperation.CHI;case 2->LongCardOperation.PENG;case 1,9,75->LongCardOperation.HU;case 8->LongCardOperation.PASS;case 96,99,124,125->LongCardOperation.CALL;default->throw new IllegalArgumentException("unsupported AYDSS operation: "+op);};state=BuiltinLongCardFamilies.require("AYDSS").engine().apply(state,new LongCardCommand(operation,request.seatId(),cards),new LongCardRuleContext(number(payload.asMap().get("huPoints"),0)));}
    @Override public synchronized Map<String,Object> viewFor(long viewer){Integer own=seat(viewer);Map<Integer,Object>visible=new LinkedHashMap<>();seats.forEach((seat,id)->{List<Integer>cards=state==null?List.of():state.hands().get(seat);visible.put(seat,Map.of("playerId",id,"ready",ready.contains(seat),"cards",Objects.equals(own,seat)||state!=null&&state.phase()==LongCardPhase.FINISHED?cards:Collections.nCopies(cards.size(),0),"cardCount",cards.size()));});Map<String,Object>out=new LinkedHashMap<>();out.put("roomID",roomId);out.put("roomId",roomId);out.put("gameCode","aydss");out.put("started",state!=null);out.put("currentSeat",state==null?-1:state.currentSeat());out.put("phase",state==null?"WAITING":state.phase().name());out.put("winnerSeat",state==null||state.winnerSeat()==null?-1:state.winnerSeat());out.put("seats",Map.copyOf(visible));out.put("stateVersion",stateVersion);return Map.copyOf(out);}
    @Override public synchronized long stateVersion(){return stateVersion;}
    @Override public synchronized Map<String,Object> authoritativeState(){return snapshot();}
    synchronized List<Object> recordedEvents(){return List.copyOf(events);}
    private Map<String,Object> snapshot(){Map<String,Object>out=new LinkedHashMap<>();out.put("schemaVersion",1);out.put("roomId",roomId);out.put("ownerId",ownerId);out.put("players",Map.copyOf(seats));out.put("ready",List.copyOf(ready));out.put("state",state==null?"WAITING":encodeState(state));out.put("rules",rules.asMap());out.put("lastSequence",lastSequence);out.put("stateVersion",stateVersion);return Map.copyOf(out);}
    static StatePayload replay(StatePayload base,List<Object> orderedEvents){Map<String,Object> current=new LinkedHashMap<>(base.asMap());long version=longNumber(current.getOrDefault("stateVersion",0));for(Object raw:orderedEvents){Map<String,Object>event=stringMap(raw);long next=longNumber(event.get("version"));if(next!=version+1)throw new IllegalArgumentException("AYDSS event version gap");current=new LinkedHashMap<>(stringMap(event.get("after")));if(longNumber(current.get("stateVersion"))!=next)throw new IllegalArgumentException("AYDSS event payload version mismatch");version=next;}return StatePayload.copyOf(current);}
    private static Map<String,Object> encodeState(LongCardState s){return Map.of("hands",s.hands(),"deck",s.deck(),"discards",s.discards(),"currentSeat",s.currentSeat(),"phase",s.phase().name(),"exposedCard",s.exposedCard()==null?-1:s.exposedCard(),"calledSeats",s.calledSeats(),"winnerSeat",s.winnerSeat()==null?-1:s.winnerSeat());}
    private static LongCardState decodeState(Map<String,Object> map){Map<Integer,List<Integer>>hands=new LinkedHashMap<>();stringMap(map.get("hands")).forEach((seat,cards)->hands.put(Integer.parseInt(seat),collection(cards).stream().map(v->number(v,-1)).toList()));int exposed=number(map.get("exposedCard"),-1),winner=number(map.get("winnerSeat"),-1);return new LongCardState(hands,collection(map.get("deck")).stream().map(v->number(v,-1)).toList(),collection(map.get("discards")).stream().map(v->number(v,-1)).toList(),number(map.get("currentSeat"),-1),LongCardPhase.valueOf(String.valueOf(map.get("phase"))),exposed<0?null:exposed,new LinkedHashSet<>(collection(map.get("calledSeats")).stream().map(v->number(v,-1)).toList()),winner<0?null:winner);}
    @Override public OperationDeadline operationDeadline(){return OperationDeadline.none();}
    @Override public OperationDeadlineArbiter deadlineArbiter(){return arbiter;}
    @Override public synchronized List<String> invariantViolations(){List<String>v=new ArrayList<>();if(!seats.containsValue(ownerId))v.add("OWNER_NOT_SEATED");if(new HashSet<>(seats.values()).size()!=seats.size())v.add("DUPLICATE_PLAYER");if(!seats.keySet().containsAll(ready))v.add("READY_WITHOUT_SEAT");if(state!=null&&!state.hands().keySet().equals(seats.keySet()))v.add("HAND_SEAT_MISMATCH");return List.copyOf(v);}
    @Override public synchronized SettlementPayload settlement(int round,String version){if(state==null||state.phase()!=LongCardPhase.FINISHED)throw new IllegalStateException("round not finished");if(!AydssGameProvider.VERSION.equals(version))throw new IllegalArgumentException("play version mismatch");Map<Long,Long>delta=new LinkedHashMap<>();seats.values().forEach(id->delta.put(id,0L));return new SettlementPayload(roomId,round,version,delta);}
    private void own(int seat,long player){if(!Long.valueOf(player).equals(seats.get(seat)))throw new SecurityException("seat not owned");}private void requireParticipant(long player){if(!seats.containsValue(player))throw new SecurityException("player not in room");}private Integer seat(long player){return seats.entrySet().stream().filter(e->e.getValue()==player).map(Map.Entry::getKey).findFirst().orElse(null);}private static long player(String id){long value=Long.parseLong(id);if(value<=0)throw new IllegalArgumentException("invalid player");return value;}
    private static String action(CommandPayload body){Object value=body.asMap().get("action");return value==null?"state":String.valueOf(value);}
    private static CommandPayload payload(CommandPayload body){Object value=body.asMap().get("payload");return value instanceof Map<?,?> map?CommandPayload.copyOf((Map<String,Object>)map):CommandPayload.empty();}
    private static int number(Object value,int fallback){return value==null?fallback:value instanceof Number n?n.intValue():Integer.parseInt(String.valueOf(value));}
    private static long longNumber(Object value){if(value==null)throw new IllegalArgumentException("missing AYDSS snapshot number");return value instanceof Number n?n.longValue():Long.parseLong(String.valueOf(value));}
    private static Collection<?> collection(Object value){if(!(value instanceof Collection<?> c))throw new IllegalArgumentException("invalid AYDSS snapshot collection");return c;}
    @SuppressWarnings("unchecked") private static Map<String,Object> stringMap(Object value){if(!(value instanceof Map<?,?> map))throw new IllegalArgumentException("invalid AYDSS snapshot map");Map<String,Object>out=new LinkedHashMap<>();map.forEach((key,item)->out.put(String.valueOf(key),item));return out;}
}
