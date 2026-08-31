package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.GameCommandResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Post-upgrade V2 frame adapter. It owns transport concerns only and delegates every command to the real router. */
public final class GatewayWebSocketFrameHandler extends SimpleChannelInboundHandler<io.netty.handler.codec.http.websocketx.WebSocketFrame> {
    @FunctionalInterface public interface SessionResolver {
        SessionBinding resolve(ConnectionIdentity identity, WebSocketFrame frame);
    }
    public record SessionBinding(long accountId,ConnectionSession session) {
        public SessionBinding { if(accountId<=0)throw new IllegalArgumentException("invalid account binding");Objects.requireNonNull(session); }
    }
    @FunctionalInterface public interface BroadcastSink {
        Iterable<Broadcast> publish(ConnectionSession session, WebSocketFrame request, GameCommandResult result);
        default void connected(ChannelHandlerContext context,ConnectionIdentity identity,ConnectionSession session){}
        default void disconnected(ChannelHandlerContext context){}
        static BroadcastSink none() { return (session, request, result) -> java.util.List.of(); }
    }
    public record Broadcast(String msgId,String requestId,Map<String,Object> body) {
        public Broadcast { if(msgId==null||msgId.isBlank()||requestId==null||requestId.isBlank())throw new IllegalArgumentException("invalid broadcast");body=Map.copyOf(body==null?Map.of():body); }
    }
    @FunctionalInterface public interface Factory {
        GatewayWebSocketFrameHandler create(ConnectionIdentity identity);
    }
    @FunctionalInterface public interface NonRoomDispatcher {
        Map<String,Object> dispatch(ConnectionIdentity identity, WebSocketFrame frame);
        static NonRoomDispatcher rejecting() { return (identity, frame) -> { throw new IllegalArgumentException("non-room message is unsupported"); }; }
    }

    private static final TypeReference<Map<String,Object>> MAP = new TypeReference<>() { };
    private final ConnectionIdentity identity;
    private final GameWebSocketRouter router;
    private final SessionResolver sessions;
    private final BroadcastSink broadcasts;
    private final NonRoomDispatcher nonRoom;
    private final ObjectMapper json;
    private final Clock clock;
    private ConnectionSession session;

    public GatewayWebSocketFrameHandler(ConnectionIdentity identity, GameWebSocketRouter router,
            SessionResolver sessions, BroadcastSink broadcasts, ObjectMapper json, Clock clock) {
        this(identity,router,sessions,broadcasts,NonRoomDispatcher.rejecting(),json,clock);
    }

    public GatewayWebSocketFrameHandler(ConnectionIdentity identity, GameWebSocketRouter router,
            SessionResolver sessions, BroadcastSink broadcasts, NonRoomDispatcher nonRoom, ObjectMapper json, Clock clock) {
        this.identity=Objects.requireNonNull(identity);this.router=Objects.requireNonNull(router);
        this.sessions=Objects.requireNonNull(sessions);this.broadcasts=Objects.requireNonNull(broadcasts);
        this.nonRoom=Objects.requireNonNull(nonRoom);
        this.json=Objects.requireNonNull(json);this.clock=Objects.requireNonNull(clock);
    }

    @Override protected void channelRead0(ChannelHandlerContext ctx, io.netty.handler.codec.http.websocketx.WebSocketFrame wire) {
        if (wire instanceof PingWebSocketFrame ping) { ctx.writeAndFlush(new PongWebSocketFrame(ping.content().retain())); return; }
        if (wire instanceof PongWebSocketFrame) return;
        if (wire instanceof CloseWebSocketFrame close) { ctx.writeAndFlush(close.retain()).addListener(ChannelFutureListener.CLOSE); return; }
        if (wire instanceof BinaryWebSocketFrame) { close(ctx, WebSocketCloseStatus.INVALID_MESSAGE_TYPE, "text JSON required"); return; }
        if (!(wire instanceof TextWebSocketFrame text)) { close(ctx, WebSocketCloseStatus.PROTOCOL_ERROR, "unsupported frame"); return; }
        WebSocketFrame request=null;
        try {
            Map<String,Object> raw=json.readValue(text.text(),MAP);
            request=json.convertValue(raw,WebSocketFrame.class);
            if ("gateway.heartbeat".equals(request.msgId())) {
                heartbeat(ctx, request);
                return;
            }
            if (WebSocketFrame.isNonRoomMessage(request.msgId())) {
                success(ctx, request, nonRoom.dispatch(identity, request), false);
                return;
            }
            if(session==null){SessionBinding binding=sessions.resolve(identity,request);if(binding.accountId()!=identity.userId())throw new SecurityException("ticket/session identity mismatch");session=binding.session();broadcasts.connected(ctx,identity,session);}
            GameWebSocketRouter.RoutedResult routed=router.route(session,request);session=routed.session();
            GameCommandResult result=routed.result();
            Map<String,Object> response=new LinkedHashMap<>();
            response.put("protocolVersion","2.0");response.put("msgId",request.msgId());response.put("kind","resp");
            response.put("requestId",request.requestId());response.put("seq",request.seq());response.put("timestamp",clock.millis());
            response.put("traceId",request.traceId());response.put("code",0);response.put("message",routed.replayed()?"replayed":"OK");
            response.put("body",result.body().asMap());
            ctx.writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(response)));
            if(!routed.replayed())for(Broadcast push:broadcasts.publish(session,request,result)){
                Map<String,Object> event=new LinkedHashMap<>();event.put("protocolVersion","2.0");event.put("msgId",push.msgId());event.put("kind","push");event.put("requestId",push.requestId());event.put("seq",request.seq());event.put("timestamp",clock.millis());event.put("traceId",request.traceId());event.put("body",push.body());ctx.writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(event)));
            }
        } catch (SecurityException failure) {
            failure(ctx, GatewayErrorCode.UNAUTHORIZED, request, true);
        } catch (GameWebSocketRouter.RequestOutcomeUnknownException failure) {
            failure(ctx, GatewayErrorCode.IDEMPOTENCY_CONFLICT, request, false);
        } catch (IllegalArgumentException failure) {
            String trace=request==null?"unknown":request.traceId(),message=request==null?"unknown":request.msgId();
            System.err.printf("gateway websocket request rejected trace=%s msgId=%s cause=%s%n",trace,message,String.valueOf(failure.getMessage()));
            failure(ctx, GatewayErrorCode.INVALID_ENVELOPE, request, true);
        } catch (Exception failure) {
            String trace=request==null?"unknown":request.traceId(),message=request==null?"unknown":request.msgId();
            System.err.printf("gateway websocket command failed trace=%s msgId=%s cause=%s:%s%n",trace,message,
                    failure.getClass().getSimpleName(),String.valueOf(failure.getMessage()));
            failure.printStackTrace(System.err);
            failure(ctx, GatewayErrorCode.BACKPRESSURE, request, true);
        }
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { close(ctx,WebSocketCloseStatus.INTERNAL_SERVER_ERROR,"gateway failure"); }
    @Override public void channelInactive(ChannelHandlerContext ctx){broadcasts.disconnected(ctx);}

    private void heartbeat(ChannelHandlerContext ctx, WebSocketFrame request) throws Exception {
        success(ctx, request, Map.of("serverTime",clock.millis()), false);
    }

    private void success(ChannelHandlerContext ctx, WebSocketFrame request, Map<String,Object> body, boolean replayed) throws Exception {
        Map<String,Object> response=new LinkedHashMap<>();
        response.put("protocolVersion","2.0");response.put("msgId",request.msgId());response.put("kind","resp");
        response.put("requestId",request.requestId());response.put("seq",request.seq());response.put("timestamp",clock.millis());
        response.put("traceId",request.traceId());response.put("code",0);response.put("message",replayed?"replayed":"OK");
        response.put("body",body==null?Map.of():body);
        ctx.writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(response)));
    }

    private void failure(ChannelHandlerContext ctx,GatewayErrorCode code,WebSocketFrame request,boolean close) {
        try {
            String requestId=request==null?"unknown":request.requestId(),traceId=request==null?"unknown":request.traceId(),msgId=request==null?"gateway.error":request.msgId();long seq=request==null?0:request.seq();
            Map<String,Object> response=Map.of("protocolVersion","2.0","msgId",msgId,"kind","resp",
                    "requestId",requestId,"seq",seq,"timestamp",clock.millis(),"traceId",traceId,
                    "code",code.code(),"message",code.defaultMessage(),"body",Map.of());
            var future=ctx.writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(response)));
            if(close)future.addListener(ignored -> close(ctx,WebSocketCloseStatus.POLICY_VIOLATION,code.name()));
        } catch(Exception ignored) { ctx.close(); }
    }
    private static void close(ChannelHandlerContext ctx,WebSocketCloseStatus status,String reason) {
        ctx.writeAndFlush(new CloseWebSocketFrame(status.code(),reason)).addListener(ChannelFutureListener.CLOSE);
    }
}
