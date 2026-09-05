package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

final class GatewaySessionRegistryTest {
    @Test void replacesOlderSessionEvenWhenBothPagesShareTheSameDevice() throws Exception {
        var registry=new GatewaySessionRegistry();
        var oldDevice=new EmbeddedChannel();
        var currentDevice=new EmbeddedChannel();
        registry.connected(oldDevice,new ConnectionIdentity(42,"shared-browser","https://game.example","old-session",7,"old-page"),new ObjectMapper(),Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));
        registry.connected(currentDevice,new ConnectionIdentity(43,"shared-browser","https://game.example","new-session",8,"new-page"),new ObjectMapper(),Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));

        assertEquals(1,registry.replace(42,"new-session",8,new ObjectMapper(),Clock.fixed(Instant.EPOCH,ZoneOffset.UTC)));
        TextWebSocketFrame event=oldDevice.readOutbound();
        JsonNode envelope=decodeLikeProtocolClient(new ObjectMapper(),event.text());
        assertEquals("system.kick_out",envelope.path("msgId").asText());
        assertEquals("SESSION_REPLACED",envelope.path("body").path("payload").path("reasonCode").asText());
        assertEquals("你的账号已在其他设备登录",envelope.path("body").path("payload").path("message").asText());
        event.release();
        CloseWebSocketFrame close=oldDevice.readOutbound();
        assertEquals(GatewaySessionRegistry.SESSION_REPLACED_CLOSE_CODE,close.statusCode());
        close.release();
        assertNull(currentDevice.readOutbound());
        oldDevice.finishAndReleaseAll();
        currentDevice.finishAndReleaseAll();
    }

    @Test void secondPageReplacesFirstEvenWhenSessionAndDeviceAreShared() throws Exception {
        var registry=new GatewaySessionRegistry();
        var first=new EmbeddedChannel();var second=new EmbeddedChannel();
        var firstIdentity=new ConnectionIdentity(42,"same-device","https://game.example","same-session",9,"first-page");
        var secondIdentity=new ConnectionIdentity(42,"same-device","https://game.example","same-session",9,"second-page");
        var json=new ObjectMapper();var clock=Clock.fixed(Instant.EPOCH,ZoneOffset.UTC);
        assertEquals(0,registry.connected(first,firstIdentity,json,clock));
        assertEquals(1,registry.connected(second,secondIdentity,json,clock));
        TextWebSocketFrame event=first.readOutbound();
        assertEquals("SESSION_REPLACED",decodeLikeProtocolClient(json,event.text()).path("body").path("payload").path("reasonCode").asText());event.release();
        CloseWebSocketFrame close=first.readOutbound();assertEquals(4001,close.statusCode());close.release();
        registry.disconnected(first);
        assertNull(second.readOutbound());
        first.finishAndReleaseAll();second.finishAndReleaseAll();
    }

    @Test void concurrentConnectionsLeaveOnlyTheLastSerializedChannelWithoutDeletingItOnOldDisconnect() throws Exception {
        var registry=new GatewaySessionRegistry();var json=new ObjectMapper();var clock=Clock.fixed(Instant.EPOCH,ZoneOffset.UTC);
        var first=new EmbeddedChannel();var second=new EmbeddedChannel();
        var firstIdentity=new ConnectionIdentity(42,"same-device","https://game.example","same-session",9,"first-page");
        var secondIdentity=new ConnectionIdentity(42,"same-device","https://game.example","same-session",9,"second-page");
        var start=new java.util.concurrent.CountDownLatch(1);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{
            var a=pool.submit(()->{start.await();return registry.connected(first,firstIdentity,json,clock);});
            var b=pool.submit(()->{start.await();return registry.connected(second,secondIdentity,json,clock);});
            start.countDown();assertEquals(1,a.get()+b.get());
            int firstFrames=first.outboundMessages().size(),secondFrames=second.outboundMessages().size();
            assertTrue((firstFrames==2&&secondFrames==0)||(firstFrames==0&&secondFrames==2));
            EmbeddedChannel old=firstFrames==2?first:second,current=firstFrames==0?first:second;
            registry.disconnected(old);assertNull(current.readOutbound());
        }finally{pool.shutdownNow();first.finishAndReleaseAll();second.finishAndReleaseAll();}
    }

    /** 与 ProtocolClient.decodeWire/dispatchV2Push 保持相同的 fail-closed 契约。 */
    private static JsonNode decodeLikeProtocolClient(ObjectMapper json,String wire) throws Exception {
        JsonNode envelope=json.readTree(wire);
        assertEquals("2.0",envelope.path("protocolVersion").asText());
        assertEquals("push",envelope.path("kind").asText());
        assertTrue(envelope.path("seq").canConvertToLong()&&envelope.path("seq").asLong()>0);
        assertTrue(envelope.path("timestamp").canConvertToLong()&&envelope.path("timestamp").asLong()>0);
        assertFalse(envelope.path("requestId").asText().isBlank());
        assertFalse(envelope.path("traceId").asText().isBlank());
        assertEquals("system.kick_out",envelope.path("body").path("action").asText());
        assertTrue(envelope.path("body").has("payload"));
        return envelope;
    }
}
