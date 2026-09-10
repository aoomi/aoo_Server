package com.aoo.bcg.gateway;

import com.aoo.bcg.common.idempotency.InMemoryIdempotencyStore;
import com.aoo.bcg.gamespi.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class GatewayHandshakeIntegrationTest {
    @Test void authenticatedUpgradeInstallsBusinessHandlerAndFrameReachesRouter() throws Exception {
        Instant now=Instant.parse("2026-08-24T12:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);JdbcDataSource source=schema(clock);
        JdbcWsTicketService tickets=new JdbcWsTicketService(source,clock);String ticket=tickets.issue("access-secret","device-a","https://game.example","page-upgrade","upgrade-1").ticket();
        GameRoomHandle room=new GameRoomHandle(100,62,"v1",new Object());GameRegistry games=new GameRegistry();games.register(provider(room));
        var router=new GameWebSocketRouter(games,id->room,new WebSocketRequestGuard(clock,Duration.ofSeconds(30)),new InMemoryIdempotencyStore<>(clock),Duration.ofHours(24));
        ObjectMapper json=new ObjectMapper();GatewayWebSocketFrameHandler.Factory factory=identity->new GatewayWebSocketFrameHandler(identity,router,(principal,frame)->new GatewayWebSocketFrameHandler.SessionBinding(principal.userId(),new ConnectionSession(Long.toString(principal.userId()),frame.roomId(),0,frame.playVersion(),0)),GatewayWebSocketFrameHandler.BroadcastSink.none(),json,clock);
        EmbeddedChannel channel=new EmbeddedChannel(new HttpServerCodec(),new HttpObjectAggregator(16*1024),new GatewayApplication.Ingress(tickets,json,Set.of("https://game.example"),Runnable::run,factory));
        FullHttpRequest upgrade=new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,HttpMethod.GET,"/api/v2/gateway/ws");
        upgrade.headers().set(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL,"aoo.v2, aoo.ticket."+ticket+", aoo.page.page-upgrade");
        upgrade.headers().set(HttpHeaderNames.HOST,"game.example").set(HttpHeaderNames.ORIGIN,"https://game.example").set("X-Forwarded-Proto","https").set(HttpHeaderNames.UPGRADE,"websocket").set(HttpHeaderNames.CONNECTION,"Upgrade").set(HttpHeaderNames.SEC_WEBSOCKET_VERSION,"13").set(HttpHeaderNames.SEC_WEBSOCKET_KEY,"dGhlIHNhbXBsZSBub25jZQ==");
        channel.writeInbound(upgrade);Object handshake=channel.readOutbound();assertNotNull(handshake);io.netty.util.ReferenceCountUtil.release(handshake);
        assertNotNull(channel.pipeline().get("gateway-v2-business"),"business handler must exist before the client can observe handshake success");
        Map<String,Object> request=new LinkedHashMap<>();request.put("protocolVersion","2.0");request.put("msgId","poker.zypk.operate_req");request.put("kind","req");request.put("requestId","r1");request.put("seq",1);request.put("traceId","t1");request.put("roomId","100");request.put("roundNo",1);request.put("playVersion","v1");request.put("timestamp",now.toEpochMilli());request.put("body",Map.of());
        channel.writeInbound(new TextWebSocketFrame(json.writeValueAsString(request)));
        Object response=channel.readOutbound();assertNotNull(response);io.netty.util.ReferenceCountUtil.release(response);channel.finishAndReleaseAll();
    }

    private static GameProvider provider(GameRoomHandle room){return new GameProvider(){public GameDescriptor descriptor(){return new GameDescriptor(62,"zypk","ZYPK",GameCategory.POKER,"configurable",RegionScope.NATIONAL,"","",room.playVersion());}public GameRoomFactory roomFactory(){return ignored->room;}public Optional<GameCommandHandler> commandHandler(){return Optional.of((ignored,request)->new GameCommandResult("poker.zypk.operate_resp",request.requestId(),Map.of("accepted",true)));}};}
    private static JdbcDataSource schema(Clock clock)throws Exception{JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:handshake"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1");try(Connection c=ds.getConnection();Statement s=c.createStatement()){
        s.execute("CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY,auth_generation BIGINT NOT NULL,banned_until TIMESTAMP NULL)");s.execute("CREATE TABLE aoo_account_device(account_id BIGINT,device_id VARCHAR(128),revoked_at TIMESTAMP NULL,PRIMARY KEY(account_id,device_id))");s.execute("CREATE TABLE aoo_account_session(session_id CHAR(36) PRIMARY KEY,account_id BIGINT,device_id VARCHAR(128),access_hash CHAR(64),access_expires_at TIMESTAMP,auth_generation BIGINT,revoked_at TIMESTAMP NULL)");s.execute("CREATE TABLE gateway_ws_ticket(ticket_hash CHAR(64) PRIMARY KEY,account_id BIGINT,session_id CHAR(36),device_id VARCHAR(128),allowed_origin VARCHAR(255),idempotency_key VARCHAR(128),expires_at TIMESTAMP,issued_at TIMESTAMP,UNIQUE(account_id,idempotency_key))");s.execute("CREATE TABLE risk_event(risk_event_id BIGINT PRIMARY KEY,player_id BIGINT,room_id BIGINT,risk_score SMALLINT,status VARCHAR(24),created_at TIMESTAMP)");s.execute("CREATE TABLE telemetry_signal(signal_id BIGINT PRIMARY KEY,player_id BIGINT,room_id BIGINT,device_hash CHAR(64),geo_cell VARCHAR(32),received_at TIMESTAMP)");s.execute("CREATE TABLE risk_admission_decision(request_id VARCHAR(128),action_name VARCHAR(24),player_id BIGINT,device_hash CHAR(64),room_id BIGINT,decision_code VARCHAR(16),risk_score SMALLINT,reason_codes VARCHAR(255),decided_at TIMESTAMP,PRIMARY KEY(request_id,action_name))");s.execute("INSERT INTO aoo_account VALUES(7,3,NULL)");s.execute("INSERT INTO aoo_account_device VALUES(7,'device-a',NULL)");try(PreparedStatement p=c.prepareStatement("INSERT INTO aoo_account_session VALUES('00000000-0000-0000-0000-000000000007',7,'device-a',?,?,3,NULL)")){p.setString(1,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest("access-secret".getBytes(StandardCharsets.UTF_8))));p.setTimestamp(2,Timestamp.from(clock.instant().plusSeconds(600)));p.executeUpdate();}}
        return ds;}
}
