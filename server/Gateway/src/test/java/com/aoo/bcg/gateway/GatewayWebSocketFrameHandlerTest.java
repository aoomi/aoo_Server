package com.aoo.bcg.gateway;

import com.aoo.bcg.common.idempotency.InMemoryIdempotencyStore;
import com.aoo.bcg.gamespi.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class GatewayWebSocketFrameHandlerTest {
    @Test void heartbeatDoesNotRequireRoomSession() throws Exception {
        Instant now=Instant.parse("2026-08-24T12:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);
        AtomicInteger resolutions=new AtomicInteger();ObjectMapper json=new ObjectMapper();
        GameRegistry games=new GameRegistry();
        var router=new GameWebSocketRouter(games,id->{throw new AssertionError("room lookup not expected");},new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofHours(24));
        var handler=new GatewayWebSocketFrameHandler(new ConnectionIdentity(7,"d","https://game.example","test-page"),router,(identity,frame)->{resolutions.incrementAndGet();throw new AssertionError("session resolution not expected");},GatewayWebSocketFrameHandler.BroadcastSink.none(),json,clock);
        EmbeddedChannel channel=new EmbeddedChannel(handler);
        String request=json.writeValueAsString(Map.of("protocolVersion","2.0","msgId","gateway.heartbeat","kind","req","requestId","heartbeat-1","seq",1,"traceId","trace-heartbeat","timestamp",now.toEpochMilli(),"body",Map.of("clientTime",now.toEpochMilli())));
        assertFalse(channel.writeInbound(new TextWebSocketFrame(request)));
        TextWebSocketFrame response=channel.readOutbound();Map<?,?> envelope=json.readValue(response.text(),Map.class);response.release();
        assertEquals(0,envelope.get("code"));assertEquals("gateway.heartbeat",envelope.get("msgId"));assertEquals(0,resolutions.get());
        channel.finishAndReleaseAll();
    }

    @Test void accountSessionUsesNonRoomDispatcherWithoutResolvingGameRoom() throws Exception {
        Instant now=Instant.parse("2026-08-24T12:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);ObjectMapper json=new ObjectMapper();AtomicInteger resolutions=new AtomicInteger();
        var router=new GameWebSocketRouter(new GameRegistry(),id->{throw new AssertionError("room lookup not expected");},new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofHours(24));
        var handler=new GatewayWebSocketFrameHandler(new ConnectionIdentity(7,"d","https://game.example","test-page"),router,(identity,frame)->{resolutions.incrementAndGet();throw new AssertionError("session resolution not expected");},GatewayWebSocketFrameHandler.BroadcastSink.none(),(identity,frame)->Map.of("pid",identity.userId()),json,clock);
        EmbeddedChannel channel=new EmbeddedChannel(handler);String request=json.writeValueAsString(Map.of("protocolVersion","2.0","msgId","account.session_dispatch","kind","req","requestId","login-1","seq",1,"traceId","trace-login","timestamp",now.toEpochMilli(),"body",Map.of("action","base.C1006RoleLogin","payload",Map.of())));
        channel.writeInbound(new TextWebSocketFrame(request));TextWebSocketFrame response=channel.readOutbound();Map<?,?> envelope=json.readValue(response.text(),Map.class);response.release();
        assertEquals(0,envelope.get("code"));assertEquals(Map.of("pid",7),envelope.get("body"));assertEquals(0,resolutions.get());channel.finishAndReleaseAll();
    }

    @Test void lobbyAndClubMessagesNeverEnterRoomAuthorityWhileRoomMessagesFailClosed() throws Exception {
        Instant now=Instant.parse("2026-08-24T12:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);ObjectMapper json=new ObjectMapper();AtomicInteger resolutions=new AtomicInteger(),nonRoomCalls=new AtomicInteger();
        var router=new GameWebSocketRouter(new GameRegistry(),id->{throw new AssertionError("room lookup not expected");},new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofHours(24));
        var handler=new GatewayWebSocketFrameHandler(new ConnectionIdentity(7,"d","https://game.example","test-page"),router,(identity,frame)->{resolutions.incrementAndGet();throw new AssertionError("room session not expected");},GatewayWebSocketFrameHandler.BroadcastSink.none(),(identity,frame)->{nonRoomCalls.incrementAndGet();return Map.of("action",frame.body().get("action"));},json,clock);
        EmbeddedChannel channel=new EmbeddedChannel(handler);
        for(String msgId:List.of("hall.dispatch","club.dispatch")){String request=json.writeValueAsString(Map.of("protocolVersion","2.0","msgId",msgId,"kind","req","requestId",msgId,"seq",nonRoomCalls.get()+1,"traceId","trace-"+msgId,"timestamp",now.toEpochMilli(),"body",Map.of("action","catalog","payload",Map.of())));channel.writeInbound(new TextWebSocketFrame(request));TextWebSocketFrame response=channel.readOutbound();assertEquals(0,json.readValue(response.text(),Map.class).get("code"));response.release();}
        assertEquals(2,nonRoomCalls.get());assertEquals(0,resolutions.get());channel.finishAndReleaseAll();
        assertThrows(IllegalArgumentException.class,()->new WebSocketFrame("2.0","common.room.dispatch","req","missing-room",3,"trace-room",null,0,null,now.toEpochMilli(),Map.of()));
    }

    @Test void realTextFramesReachRouterCorrelateResponsesBroadcastOnceAndReplayIdempotently() throws Exception {
        Instant now=Instant.parse("2026-08-24T12:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);
        AtomicInteger executions=new AtomicInteger(),broadcasts=new AtomicInteger();
        GameRoomHandle room=new GameRoomHandle(100,62,"zypk@1.0.0",new Object());
        GameProvider provider=provider(room,executions);GameRegistry games=new GameRegistry();games.register(provider);
        GameWebSocketRouter router=new GameWebSocketRouter(games,id->room,new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofHours(24));
        ObjectMapper json=new ObjectMapper();ConnectionIdentity identity=new ConnectionIdentity(7,"device","https://game.example","test-page");
        var handler=new GatewayWebSocketFrameHandler(identity,router,(principal,frame)->new GatewayWebSocketFrameHandler.SessionBinding(principal.userId(),new ConnectionSession(Long.toString(principal.userId()),frame.roomId(),0,frame.playVersion(),0)),(session,request,result)->{broadcasts.incrementAndGet();return List.of(new GatewayWebSocketFrameHandler.Broadcast("common.room.state_push",request.requestId(),result.body().asMap()));},json,clock);
        EmbeddedChannel channel=new EmbeddedChannel(handler);
        String request=json.writeValueAsString(Map.ofEntries(Map.entry("protocolVersion","2.0"),Map.entry("msgId","poker.zypk.operate_req"),Map.entry("kind","req"),Map.entry("requestId","r-1"),Map.entry("seq",1),Map.entry("traceId","t-1"),Map.entry("roomId","100"),Map.entry("roundNo",1),Map.entry("playVersion","zypk@1.0.0"),Map.entry("timestamp",now.toEpochMilli()),Map.entry("body",Map.of())));
        assertFalse(channel.writeInbound(new TextWebSocketFrame(request)));
        TextWebSocketFrame response=channel.readOutbound();Map<?,?> envelope=json.readValue(response.text(),Map.class);response.release();
        assertEquals("resp",envelope.get("kind"));assertEquals("r-1",envelope.get("requestId"));assertEquals("t-1",envelope.get("traceId"));assertEquals("poker.zypk.operate_req",envelope.get("msgId"));assertEquals(1,envelope.get("seq"));assertEquals(0,envelope.get("code"));
        TextWebSocketFrame push=channel.readOutbound();Map<?,?> pushEnvelope=json.readValue(push.text(),Map.class);push.release();assertEquals("push",pushEnvelope.get("kind"));assertEquals("r-1",pushEnvelope.get("requestId"));
        String retry=request.replace("\"seq\":1","\"seq\":2");
        channel.writeInbound(new TextWebSocketFrame(retry));TextWebSocketFrame replay=channel.readOutbound();Map<?,?> replayEnvelope=json.readValue(replay.text(),Map.class);replay.release();
        assertEquals("replayed",replayEnvelope.get("message"));assertEquals(1,executions.get());assertEquals(1,broadcasts.get());channel.finishAndReleaseAll();
    }

    @Test void forgedVersionAndSequenceViolationProducesRetryableMachineFailure() throws Exception {
        Instant now=Instant.parse("2026-08-24T12:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);GameRoomHandle room=new GameRoomHandle(100,62,"v1",new Object());
        GameRegistry games=new GameRegistry();games.register(provider(room,new AtomicInteger()));ObjectMapper json=new ObjectMapper();
        var handler=new GatewayWebSocketFrameHandler(new ConnectionIdentity(7,"d","https://game.example","test-page"),new GameWebSocketRouter(games,id->room,new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofHours(24)),(id,frame)->new GatewayWebSocketFrameHandler.SessionBinding(id.userId(),new ConnectionSession("7","100",0,"v1",0)),GatewayWebSocketFrameHandler.BroadcastSink.none(),json,clock);
        EmbeddedChannel channel=new EmbeddedChannel(handler);
        String forged=json.writeValueAsString(Map.ofEntries(Map.entry("protocolVersion","2.0"),Map.entry("msgId","poker.zypk.operate_req"),Map.entry("kind","req"),Map.entry("requestId","r-x"),Map.entry("seq",2),Map.entry("traceId","t-x"),Map.entry("roomId","100"),Map.entry("roundNo",1),Map.entry("playVersion","v2"),Map.entry("timestamp",now.toEpochMilli()),Map.entry("body",Map.of())));
        channel.writeInbound(new TextWebSocketFrame(forged));assertInstanceOf(TextWebSocketFrame.class,channel.readOutbound());assertTrue(channel.isActive());assertNull(channel.readOutbound());channel.finishAndReleaseAll();
    }

    @Test void businessCommandFailureRespondsWithCorrelationAndKeepsSocketOpen() throws Exception {
        Instant now=Instant.parse("2026-08-24T12:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);
        GameRoomHandle room=new GameRoomHandle(100,62,"v1",new Object());
        AtomicInteger attempts=new AtomicInteger();
        GameRegistry games=new GameRegistry();games.register(new GameProvider(){
            public GameDescriptor descriptor(){return new GameDescriptor(62,"zypk","ZYPK",GameCategory.POKER,"configurable",RegionScope.NATIONAL,"","",room.playVersion());}
            public GameRoomFactory roomFactory(){return ignored->room;}
            public Optional<GameCommandHandler> commandHandler(){return Optional.of((ignored,request)->{
                if(attempts.incrementAndGet()==1)throw new IllegalStateException("not current seat");
                return new GameCommandResult("common.room.play_resp",request.requestId(),Map.of("accepted",true));
            });}
        });
        ObjectMapper json=new ObjectMapper();
        var handler=new GatewayWebSocketFrameHandler(new ConnectionIdentity(7,"d","https://game.example","test-page"),
                new GameWebSocketRouter(games,id->room,new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofHours(24)),
                (id,frame)->new GatewayWebSocketFrameHandler.SessionBinding(id.userId(),new ConnectionSession("7","100",0,"v1",0)),
                GatewayWebSocketFrameHandler.BroadcastSink.none(),json,clock);
        EmbeddedChannel channel=new EmbeddedChannel(handler);
        String request=json.writeValueAsString(Map.ofEntries(Map.entry("protocolVersion","2.0"),Map.entry("msgId","common.room.play_req"),Map.entry("kind","req"),Map.entry("requestId","play-1"),Map.entry("seq",1),Map.entry("traceId","trace-play"),Map.entry("roomId","100"),Map.entry("roundNo",1),Map.entry("playVersion","v1"),Map.entry("timestamp",now.toEpochMilli()),Map.entry("body",Map.of("cards",List.of(104)))));
        channel.writeInbound(new TextWebSocketFrame(request));
        TextWebSocketFrame response=channel.readOutbound();Map<?,?> envelope=json.readValue(response.text(),Map.class);response.release();
        assertEquals("resp",envelope.get("kind"));assertEquals("play-1",envelope.get("requestId"));assertEquals("trace-play",envelope.get("traceId"));assertEquals(1,envelope.get("seq"));assertEquals(3008,envelope.get("code"));assertEquals("当前还没轮到你操作",envelope.get("message"));
        assertNull(channel.readOutbound());
        String heartbeat=json.writeValueAsString(Map.of("protocolVersion","2.0","msgId","gateway.heartbeat","kind","req","requestId","heartbeat-after-error","seq",2,"traceId","trace-heartbeat","timestamp",now.toEpochMilli(),"body",Map.of()));
        channel.writeInbound(new TextWebSocketFrame(heartbeat));TextWebSocketFrame heartbeatResponse=channel.readOutbound();Map<?,?> heartbeatEnvelope=json.readValue(heartbeatResponse.text(),Map.class);heartbeatResponse.release();
        assertEquals(0,heartbeatEnvelope.get("code"));
        String next=json.writeValueAsString(Map.ofEntries(Map.entry("protocolVersion","2.0"),Map.entry("msgId","common.room.play_req"),Map.entry("kind","req"),Map.entry("requestId","play-2"),Map.entry("seq",3),Map.entry("traceId","trace-play-2"),Map.entry("roomId","100"),Map.entry("roundNo",1),Map.entry("playVersion","v1"),Map.entry("timestamp",now.toEpochMilli()),Map.entry("body",Map.of("cards",List.of(105)))));
        channel.writeInbound(new TextWebSocketFrame(next));TextWebSocketFrame nextResponse=channel.readOutbound();Map<?,?> nextEnvelope=json.readValue(nextResponse.text(),Map.class);nextResponse.release();
        assertEquals(0,nextEnvelope.get("code"));assertEquals(Map.of("accepted",true),nextEnvelope.get("body"));assertEquals(2,attempts.get());channel.finishAndReleaseAll();
    }

    private static GameProvider provider(GameRoomHandle room,AtomicInteger executions){return new GameProvider(){
        public GameDescriptor descriptor(){return new GameDescriptor(62,"zypk","ZYPK",GameCategory.POKER,"configurable",RegionScope.NATIONAL,"","",room.playVersion());}
        public GameRoomFactory roomFactory(){return ignored->room;}
        public Optional<GameCommandHandler> commandHandler(){return Optional.of((ignored,request)->new GameCommandResult("poker.zypk.operate_resp",request.requestId(),Map.of("execution",executions.incrementAndGet())));}
    };}
}
