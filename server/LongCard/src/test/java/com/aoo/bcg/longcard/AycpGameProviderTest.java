package com.aoo.bcg.longcard;

import com.aoo.bcg.gamespi.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AycpGameProviderTest {
    @Test void creationEntryReadyStartOperationRotationReconnectSettlementAndReplay() {
        AycpGameProvider provider=new AycpGameProvider();
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(9138,10,Map.of("piao",1)));
        AycpAuthoritativeSession session=(AycpAuthoritativeSession)room.requireAuthoritativeSession();
        session.execute(command(1,1,"20",Map.of("action","join")));
        session.execute(command(2,1,"20",Map.of("action","CAYCPOpPiao","payload",Map.of("piaoHua",1))));
        session.execute(command(3,1,"20",Map.of("action","ready")));
        session.execute(command(4,0,"10",Map.of("action","start")));
        @SuppressWarnings("unchecked") Map<Integer,Object> seats=(Map<Integer,Object>)session.viewFor(10).get("seats");
        @SuppressWarnings("unchecked") Map<String,Object> owner=(Map<String,Object>)seats.get(0);
        int card=((Number)((List<?>)owner.get("cards")).getFirst()).intValue();
        session.execute(command(5,0,"10",Map.of("action","operation","payload",Map.of("opType",7,"cardID",card))));
        assertEquals(1,session.viewFor(10).get("currentSeat"),"discard must rotate authority");
        StatePayload beforeWin=StatePayload.copyOf(session.authoritativeState());
        int eventOffset=session.recordedEvents().size();
        session.execute(command(6,1,"20",Map.of("action","operation","payload",Map.of("opType",1,"huPoints",10))));
        assertEquals("FINISHED",session.viewFor(20).get("phase"));
        assertEquals(9138,session.settlement(1,AycpGameProvider.VERSION).roomId());
        AuthoritativeGameSession restored=provider.restoreAuthoritativeSession(session.authoritativeState()).orElseThrow();
        assertEquals(session.viewFor(20),restored.viewFor(20));
        StatePayload replayed=provider.eventReplayProvider().orElseThrow().replay(beforeWin,session.recordedEvents().subList(eventOffset,session.recordedEvents().size()));
        assertEquals(session.authoritativeState(),replayed.asMap());
    }
    @Test void serviceProviderRunsAuthenticatedAuthoritativeLifecycle() {
        GameProvider provider = new AycpGameProvider();
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(9138, 10, Map.of()));
        GameCommandHandler handler = provider.commandHandler().orElseThrow();
        handler.handle(room, command(1, 1, "20", Map.of("action", "join", "payload", Map.of())));
        handler.handle(room, command(2, 1, "20", Map.of("action", "CAYCPReadyRoom", "payload", Map.of())));
        GameCommandResult started = handler.handle(room, command(3, 0, "10", Map.of("action", "CAYCPStartGame", "payload", Map.of())));
        assertEquals(true, started.body().asMap().get("started"));
        @SuppressWarnings("unchecked") Map<Integer,Object> seats = (Map<Integer,Object>) started.body().asMap().get("seats");
        @SuppressWarnings("unchecked") Map<String,Object> opponent = (Map<String,Object>) seats.get(1);
        assertEquals(14, opponent.get("cardCount"));
        assertTrue(((java.util.List<?>) opponent.get("cards")).stream().allMatch(card -> card.equals(0)));
    }

    @Test void rejectsSeatSpoofingReplayAndWrongMessageFamily() {
        AycpGameProvider provider = new AycpGameProvider();
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(9002, 10, Map.of()));
        GameCommandHandler handler = provider.commandHandler().orElseThrow();
        handler.handle(room, command(1, 1, "20", Map.of("action", "join", "payload", Map.of())));
        assertThrows(SecurityException.class, () -> handler.handle(room, command(2, 1, "30", Map.of("action", "ready", "payload", Map.of()))));
        assertThrows(IllegalArgumentException.class, () -> handler.handle(room, command(1, 1, "20", Map.of("action", "ready", "payload", Map.of()))));
        RuleResult wrong = provider.ruleComponents().getFirst().execute(new GameCommandRequest("poker.dispatch", "x", 2, 9002, 0, "1.0.0", "20", 1, Map.of()));
        assertFalse(wrong.accepted());
    }

    @Test void persistedSnapshotRestoresReconnectViewWithoutHiddenStateLeak() {
        AycpGameProvider provider=new AycpGameProvider();
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(9138,10,Map.of("rounds",8)));
        GameCommandHandler handler=provider.commandHandler().orElseThrow();
        handler.handle(room,command(1,1,"20",Map.of("action","join")));
        handler.handle(room,command(2,1,"20",Map.of("action","ready")));
        handler.handle(room,command(3,0,"10",Map.of("action","start")));
        Map<String,Object> persisted=room.requireAuthoritativeSession().authoritativeState();
        AuthoritativeGameSession restored=provider.restoreAuthoritativeSession(StatePayload.copyOf(persisted)).orElseThrow();
        assertEquals(persisted,restored.authoritativeState());
        Map<String,Object> reconnect=restored.viewFor(20);
        @SuppressWarnings("unchecked") Map<Integer,Object> seats=(Map<Integer,Object>)reconnect.get("seats");
        @SuppressWarnings("unchecked") Map<String,Object> opponent=(Map<String,Object>)seats.get(0);
        assertTrue(((List<?>)opponent.get("cards")).stream().allMatch(card->card.equals(0)));
        assertTrue(restored.invariantViolations().isEmpty());
    }

    @Test void orderedAuthorityEventsReplayFromDurableSnapshotAndRejectGaps() {
        AycpGameProvider provider=new AycpGameProvider();
        AycpAuthoritativeSession session=(AycpAuthoritativeSession)provider.roomFactory().create(new RoomCreationContext(9138,10,Map.of())).requireAuthoritativeSession();
        StatePayload initial=StatePayload.copyOf(session.authoritativeState());
        session.execute(command(1,1,"20",Map.of("action","join")));
        session.execute(command(2,1,"20",Map.of("action","ready")));
        session.execute(command(3,0,"10",Map.of("action","start")));
        StatePayload replayed=provider.eventReplayProvider().orElseThrow().replay(initial,session.recordedEvents());
        assertEquals(session.authoritativeState(),replayed.asMap());
        assertEquals(session.viewFor(20),provider.restoreAuthoritativeSession(replayed).orElseThrow().viewFor(20));
        assertThrows(IllegalArgumentException.class,()->provider.eventReplayProvider().orElseThrow().replay(initial,session.recordedEvents().subList(1,3)));
    }

    private static GameCommandRequest command(long sequence,int seat,String player,Map<String,Object> body) {
        return new GameCommandRequest("longcard.aycp.dispatch", "request-"+sequence+"-"+player, sequence, 9138, 0, "1.0.0", player, seat, body);
    }
}
