package com.aoo.bcg.longcard;

import com.aoo.bcg.gamespi.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AydssGameProviderTest {
    @Test void serviceProviderRunsAuthenticatedAuthoritativeLifecycle() {
        GameProvider provider = new AydssGameProvider();
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(9001, 10, Map.of()));
        GameCommandHandler handler = provider.commandHandler().orElseThrow();
        handler.handle(room, command(1, 1, "20", Map.of("action", "join", "payload", Map.of())));
        handler.handle(room, command(2, 1, "20", Map.of("action", "CAYDSSReadyRoom", "payload", Map.of())));
        GameCommandResult started = handler.handle(room, command(3, 0, "10", Map.of("action", "CAYDSSStartGame", "payload", Map.of())));
        assertEquals(true, started.body().asMap().get("started"));
        @SuppressWarnings("unchecked") Map<Integer,Object> seats = (Map<Integer,Object>) started.body().asMap().get("seats");
        @SuppressWarnings("unchecked") Map<String,Object> opponent = (Map<String,Object>) seats.get(1);
        assertEquals(14, opponent.get("cardCount"));
        assertTrue(((java.util.List<?>) opponent.get("cards")).stream().allMatch(card -> card.equals(0)));
    }

    @Test void rejectsSeatSpoofingReplayAndWrongMessageFamily() {
        AydssGameProvider provider = new AydssGameProvider();
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(9002, 10, Map.of()));
        GameCommandHandler handler = provider.commandHandler().orElseThrow();
        handler.handle(room, command(1, 1, "20", Map.of("action", "join", "payload", Map.of())));
        assertThrows(SecurityException.class, () -> handler.handle(room, command(2, 1, "30", Map.of("action", "ready", "payload", Map.of()))));
        assertThrows(IllegalArgumentException.class, () -> handler.handle(room, command(1, 1, "20", Map.of("action", "ready", "payload", Map.of()))));
        RuleResult wrong = provider.ruleComponents().getFirst().execute(new GameCommandRequest("poker.dispatch", "x", 2, 9002, 0, "1.0.0", "20", 1, Map.of()));
        assertFalse(wrong.accepted());
    }

    @Test void persistedSnapshotRestoresReconnectViewWithoutHiddenStateLeak() {
        AydssGameProvider provider=new AydssGameProvider();
        GameRoomHandle room=provider.roomFactory().create(new RoomCreationContext(9001,10,Map.of("rounds",8)));
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
        AydssGameProvider provider=new AydssGameProvider();
        AydssAuthoritativeSession session=(AydssAuthoritativeSession)provider.roomFactory().create(new RoomCreationContext(9001,10,Map.of())).requireAuthoritativeSession();
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
        return new GameCommandRequest("longcard.aydss.dispatch", "request-"+sequence+"-"+player, sequence, 9001, 0, "1.0.0", player, seat, body);
    }
}
