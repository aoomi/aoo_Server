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
import com.aoo.bcg.gamespi.RoomMembershipLifecycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class GatewayRoomBroadcastHubTest {
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

        WebSocketFrame request = new WebSocketFrame("2.0", "poker.pdk.dispatch", "req", "request-1", 7,
                "trace-1", "9001", 3, "pdk-v1", clock.millis(), Map.of());
        assertFalse(hub.publish(firstSession, request,
                new GameCommandResult("poker.pdk.dispatch_resp", "request-1", Map.of("accepted", true))).iterator().hasNext());

        assertPerspective(json, first.readOutbound(), 11, List.of(101, 102));
        assertPerspective(json, second.readOutbound(), 22, List.of(201, 202));
        hub.disconnected(secondContext);
        assertEquals(List.of("9001:11:true","9001:22:true","9001:22:false"), presence);
        assertEquals(1, hub.connectionCount());
        hub.publish(firstSession, request,
                new GameCommandResult("poker.pdk.dispatch_resp", "request-1", Map.of("accepted", true)));
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
        WebSocketFrame request = new WebSocketFrame("2.0", "poker.pdk.dispatch", "req", "leave-1", 1,
                "trace-leave", "9001", 0, "pdk-v1", clock.millis(), Map.of());
        hub.publish(session, request, new GameCommandResult("poker.pdk.dispatch_resp", "leave-1", Map.of(
                RoomMembershipLifecycle.MEMBER_LEFT_FIELD, true,
                RoomMembershipLifecycle.MEMBER_LEFT_ACCOUNT_ID_FIELD, 22L)));
        assertEquals("9001:22:leave-1:trace-leave", completion.get());
        channel.finishAndReleaseAll();
    }

    private static void assertPerspective(ObjectMapper json, TextWebSocketFrame frame, long playerId,
            List<Integer> cards) throws Exception {
        assertNotNull(frame);
        Map<?, ?> envelope = json.readValue(frame.text(), Map.class);
        frame.release();
        assertEquals("common.room.state_push", envelope.get("msgId"));
        assertEquals("push", envelope.get("kind"));
        Map<?, ?> body = (Map<?, ?>) envelope.get("body");
        assertEquals(playerId, ((Number) body.get("viewerPlayerId")).longValue());
        assertEquals(cards, body.get("cards"));
    }
}
