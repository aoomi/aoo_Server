package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.GameCommandResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.aoo.bcg.gamespi.RoomLifecycleAuthority;
import com.aoo.bcg.gamespi.RoomMembershipLifecycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class GatewayRoomBroadcastHubTest {
    @Test void broadcastsQuickTextPayloadToEveryRoomConnection() throws Exception {
        ObjectMapper json = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC);
        GatewayRoomBroadcastHub hub = new GatewayRoomBroadcastHub((roomId, playerId) ->
                fail("quick text must not be replaced by a state snapshot"), json, clock);
        EmbeddedChannel first = new EmbeddedChannel(DefaultChannelId.newInstance(), true, false,
                new ChannelInboundHandlerAdapter());
        EmbeddedChannel second = new EmbeddedChannel(DefaultChannelId.newInstance(), true, false,
                new ChannelInboundHandlerAdapter());
        ConnectionSession sender = new ConnectionSession("11", "9001", 0, "xqp-equivalent-1", 0);
        hub.connected(first.pipeline().firstContext(), new ConnectionIdentity(11, "device-11", "https://game.example", "page-11"), sender);
        hub.connected(second.pipeline().firstContext(), new ConnectionIdentity(22, "device-22", "https://game.example", "page-22"),
                new ConnectionSession("22", "9001", 1, "xqp-equivalent-1", 0));
        WebSocketFrame request = new WebSocketFrame("2.0", "common.room.dispatch", "req", "quick-1", 4,
                "trace-quick", "9001", 1, "xqp-equivalent-1", clock.millis(),
                Map.of("action", "quick_text", "quickId", 1));
        Map<String,Object> payload = Map.of("requestId", "quick-1", "sourceSeatId", 0, "quickId", 1);

        hub.publish(sender, request, new GameCommandResult("common.room.dispatch_resp", "quick-1", payload));

        assertQuickText(json, first.readOutbound(), payload);
        assertQuickText(json, second.readOutbound(), payload);
        first.finishAndReleaseAll();
        second.finishAndReleaseAll();
    }

    @Test void broadcastsOneServerProducedPerspectivePerActiveRoomConnection() throws Exception {
        ObjectMapper json = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);
        GatewayRoomBroadcastHub hub = new GatewayRoomBroadcastHub((roomId, playerId) -> Map.of(
                "roomId", roomId,
                "viewerPlayerId", playerId,
                "cards", playerId == 11 ? List.of(101, 102) : List.of(201, 202)), json, clock);
        List<String> presence = new ArrayList<>();
        hub.configurePresenceSink((roomId,accountId,online) ->
                presence.add(roomId+":"+accountId+":"+online));
        EmbeddedChannel first = new EmbeddedChannel(DefaultChannelId.newInstance(), true, false,
                new ChannelInboundHandlerAdapter());
        EmbeddedChannel second = new EmbeddedChannel(DefaultChannelId.newInstance(), true, false,
                new ChannelInboundHandlerAdapter());
        var firstContext = first.pipeline().firstContext();
        var secondContext = second.pipeline().firstContext();
        ConnectionSession firstSession = new ConnectionSession("11", "9001", 0, "pdk-v1", 0);
        ConnectionSession secondSession = new ConnectionSession("22", "9001", 1, "pdk-v1", 0);
        hub.connected(firstContext, new ConnectionIdentity(11, "device-11", "https://game.example", "page-11"), firstSession);
        hub.connected(secondContext, new ConnectionIdentity(22, "device-22", "https://game.example", "page-22"), secondSession);

        WebSocketFrame request = new WebSocketFrame("2.0", "poker.NJ201.dispatch", "req", "request-1", 7,
                "trace-1", "9001", 3, "pdk-v1", clock.millis(), Map.of());
        assertFalse(hub.publish(firstSession, request,
                new GameCommandResult("poker.NJ201.dispatch_resp", "request-1", Map.of("accepted", true))).iterator().hasNext());

        assertPerspective(json, first.readOutbound(), 11, List.of(101, 102));
        assertPerspective(json, second.readOutbound(), 22, List.of(201, 202));
        hub.disconnected(secondContext);
        assertEquals(List.of("9001:11:true","9001:22:true","9001:22:false"), presence);
        assertEquals(1, hub.connectionCount());
        hub.publish(firstSession, request,
                new GameCommandResult("poker.NJ201.dispatch_resp", "request-1", Map.of("accepted", true)));
        assertNotNull(first.readOutbound());
        assertNull(second.readOutbound());
        first.finishAndReleaseAll();
        second.finishAndReleaseAll();
    }

    @Test void forwardsCommittedMemberExitToTheAuthoritativeHallLifecycle() {
        ObjectMapper json = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);
        GatewayRoomBroadcastHub hub = new GatewayRoomBroadcastHub((roomId, playerId) -> Map.of(), json, clock);
        AtomicReference<String> completion = new AtomicReference<>();
        hub.configureMembershipCompletion((roomId, accountId, requestId, traceId) ->
                completion.set(roomId + ":" + accountId + ":" + requestId + ":" + traceId));
        EmbeddedChannel channel = new EmbeddedChannel(DefaultChannelId.newInstance(), true, false,
                new ChannelInboundHandlerAdapter());
        var context = channel.pipeline().firstContext();
        ConnectionSession session = new ConnectionSession("22", "9001", 1, "pdk-v1", 0);
        hub.connected(context, new ConnectionIdentity(22, "device-22", "https://game.example", "page-22"), session);
        WebSocketFrame request = new WebSocketFrame("2.0", "poker.NJ201.dispatch", "req", "leave-1", 1,
                "trace-leave", "9001", 0, "pdk-v1", clock.millis(), Map.of());
        hub.publish(session, request, new GameCommandResult("poker.NJ201.dispatch_resp", "leave-1", Map.of(
                RoomMembershipLifecycle.MEMBER_LEFT_FIELD, true,
                RoomMembershipLifecycle.MEMBER_LEFT_ACCOUNT_ID_FIELD, 22L)));
        assertEquals("9001:22:leave-1:trace-leave", completion.get());
        channel.finishAndReleaseAll();
    }

    @Test void forwardsDurablyMarkedTerminalVoteToRoomClosureAfterStateBroadcast() throws Exception {
        ObjectMapper json = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);
        GatewayRoomBroadcastHub hub = new GatewayRoomBroadcastHub((roomId, playerId) -> Map.of(
                "roomId", roomId, "phase", "DISSOLVED", "dissolved", true), json, clock);
        AtomicReference<String> completion = new AtomicReference<>();
        hub.configureLifecycleCompletion((roomId, requestId, traceId, reason) ->
                completion.set(roomId + ":" + requestId + ":" + traceId + ":" + reason));
        EmbeddedChannel channel = new EmbeddedChannel(DefaultChannelId.newInstance(), true, false,
                new ChannelInboundHandlerAdapter());
        var context = channel.pipeline().firstContext();
        ConnectionSession session = new ConnectionSession("22", "9001", 1, "pdk-v1", 0);
        hub.connected(context, new ConnectionIdentity(22, "device-22", "https://game.example", "page-22"), session);
        WebSocketFrame request = new WebSocketFrame("2.0", "common.room.dissolve_agree_req", "req",
                "agree-1", 1, "trace-agree", "9001", 1, "pdk-v1", clock.millis(), Map.of());

        hub.publish(session, request, new GameCommandResult("common.room.dissolve_agree_resp", "agree-1", Map.of(
                RoomLifecycleAuthority.TERMINAL_FIELD, true,
                RoomLifecycleAuthority.TERMINAL_REASON_FIELD, "VOTE_APPROVED")));

        TextWebSocketFrame state = (TextWebSocketFrame) channel.readOutbound();
        Map<?, ?> envelope = json.readValue(state.text(), Map.class);
        state.release();
        assertEquals("common.room.state_push", envelope.get("msgId"));
        assertEquals("DISSOLVED", ((Map<?, ?>) envelope.get("body")).get("phase"));
        assertEquals("9001:agree-1:trace-agree:VOTE_APPROVED", completion.get());
        channel.finishAndReleaseAll();
    }

    @Test void terminalCompletionSurvivesPerspectiveBroadcastFailure() {
        ObjectMapper json = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);
        GatewayRoomBroadcastHub hub = new GatewayRoomBroadcastHub((roomId, playerId) -> {
            throw new IllegalStateException("simulated perspective broadcast failure");
        }, json, clock);
        AtomicReference<String> completion = new AtomicReference<>();
        hub.configureLifecycleCompletion((roomId, requestId, traceId, reason) ->
                completion.set(roomId + ":" + reason));
        EmbeddedChannel channel = new EmbeddedChannel(DefaultChannelId.newInstance(), true, false,
                new ChannelInboundHandlerAdapter());
        var context = channel.pipeline().firstContext();
        ConnectionSession session = new ConnectionSession("22", "9001", 1, "pdk-v1", 0);
        hub.connected(context, new ConnectionIdentity(22, "device-22", "https://game.example", "page-22"), session);
        WebSocketFrame request = new WebSocketFrame("2.0", "common.room.dissolve_agree_req", "req",
                "agree-2", 2, "trace-agree", "9001", 1, "pdk-v1", clock.millis(), Map.of());

        assertDoesNotThrow(() -> hub.publish(session, request,
                new GameCommandResult("common.room.dissolve_agree_resp", "agree-2", Map.of(
                        RoomLifecycleAuthority.TERMINAL_FIELD, true,
                        RoomLifecycleAuthority.TERMINAL_REASON_FIELD, "VOTE_APPROVED"))));
        assertEquals("9001:VOTE_APPROVED", completion.get());
        channel.finishAndReleaseAll();
    }

    private static void assertPerspective(ObjectMapper json, TextWebSocketFrame frame, long playerId,
            List<Integer> cards) throws Exception {
        assertNotNull(frame);
        Map<?, ?> envelope = json.readValue(frame.text(), Map.class);
        frame.release();
        assertEquals("poker.NJ201.state_push", envelope.get("msgId"));
        assertEquals("push", envelope.get("kind"));
        Map<?, ?> body = (Map<?, ?>) envelope.get("body");
        assertEquals(playerId, ((Number) body.get("viewerPlayerId")).longValue());
        assertEquals(cards, body.get("cards"));
    }

    private static void assertQuickText(ObjectMapper json, TextWebSocketFrame frame,
            Map<String,Object> payload) throws Exception {
        assertNotNull(frame);
        Map<?, ?> envelope = json.readValue(frame.text(), Map.class);
        frame.release();
        assertEquals("room.quick_text", envelope.get("msgId"));
        assertEquals(payload, envelope.get("body"));
    }
}
