package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.CommonRoomPublicViewProvider;
import com.aoo.bcg.gamespi.protocol.GeneratedProtocolIds;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import com.aoo.bcg.gamespi.RoomLifecycleAuthority;
import com.aoo.bcg.gamespi.RoomMembershipLifecycle;

/** Connection-aware room broadcaster. Every recipient receives only its server-produced perspective. */
public final class GatewayRoomBroadcastHub implements GatewayWebSocketFrameHandler.BroadcastSink {
    public record Perspective(String playVersion,Map<String,Object> body){public Perspective{if(playVersion==null||playVersion.isBlank())throw new IllegalArgumentException("perspective playVersion required");java.util.Objects.requireNonNull(body);}}
    @FunctionalInterface public interface PerspectiveView { Perspective view(long roomId,long playerId); }
    private record Binding(long playerId,long roomId,String playVersion,ChannelHandlerContext context){}
    private final Map<String,Binding> bindings=new ConcurrentHashMap<>();
    private final PerspectiveView views;private final ObjectMapper json;private final Clock clock;
    @FunctionalInterface public interface LifecycleCompletion { void complete(long roomId,String requestId,String traceId,String reason); }
    @FunctionalInterface public interface MembershipCompletion { void left(long roomId,long accountId,String requestId,String traceId); }
    @FunctionalInterface public interface PresenceSink { void update(long roomId,long accountId,boolean online); }
    private final AtomicReference<LifecycleCompletion> lifecycleCompletion=new AtomicReference<>((roomId,requestId,traceId,reason)->{});
    private final AtomicReference<MembershipCompletion> membershipCompletion=new AtomicReference<>((roomId,accountId,requestId,traceId)->{});
    private final AtomicReference<PresenceSink> presenceSink=new AtomicReference<>((roomId,accountId,online)->{});
    public GatewayRoomBroadcastHub(PerspectiveView views,ObjectMapper json,Clock clock){this.views=java.util.Objects.requireNonNull(views);this.json=java.util.Objects.requireNonNull(json);this.clock=java.util.Objects.requireNonNull(clock);}
    public static Map<String,Object> withCommonRoom(Map<String,Object> body,
            AuthoritativeGameSession session,long viewerPlayerId,boolean required){
        java.util.Objects.requireNonNull(body);java.util.Objects.requireNonNull(session);
        if(!(session instanceof CommonRoomPublicViewProvider provider)){
            if(required)throw new IllegalStateException("COMMON_ROOM_PUBLIC_VIEW_PROVIDER_REQUIRED");
            return body;
        }
        Map<String,Object> decorated=new LinkedHashMap<>(body);
        decorated.put("commonRoom",java.util.Objects.requireNonNull(
                provider.commonRoomPublicView(viewerPlayerId),"common room public view").asMap());
        return Map.copyOf(decorated);
    }
    @Override public void connected(ChannelHandlerContext context,ConnectionIdentity identity,ConnectionSession session){long room=Long.parseLong(session.roomId());bindings.put(context.channel().id().asLongText(),new Binding(identity.userId(),room,session.playVersion(),context));presenceSink.get().update(room,identity.userId(),true);}
    @Override public void disconnected(ChannelHandlerContext context){Binding binding=bindings.remove(context.channel().id().asLongText());if(binding!=null&&bindings.values().stream().noneMatch(other->other.roomId()==binding.roomId()&&other.playerId()==binding.playerId()&&other.context().channel().isActive()))presenceSink.get().update(binding.roomId(),binding.playerId(),false);}
    @Override public Iterable<GatewayWebSocketFrameHandler.Broadcast> publish(ConnectionSession session,WebSocketFrame request,GameCommandResult result){
        long roomId=Long.parseLong(session.roomId());
        for(Binding binding:bindings.values())if(binding.roomId()==roomId&&binding.context().channel().isActive())try{
            String socialEvent=socialEvent(request);
            Perspective perspective=socialEvent==null?views.view(roomId,binding.playerId()):null;
            if(socialEvent==null&&(perspective==null||!binding.playVersion().equals(perspective.playVersion())))continue;
            Map<String,Object> event=new LinkedHashMap<>();event.put("protocolVersion","2.0");event.put("msgId",socialEvent!=null?socialEvent:statePushId(request.msgId()));event.put("kind","push");event.put("requestId",request.requestId());event.put("seq",request.seq());event.put("timestamp",clock.millis());event.put("traceId",request.traceId());if(perspective!=null)event.put("playVersion",perspective.playVersion());event.put("body",perspective==null?result.body().asMap():perspective.body());binding.context().writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(event)));
        }catch(Exception failure){System.err.println("room broadcast failed roomId="+roomId+" playerId="+binding.playerId()+" cause="+failure.getMessage());}
        if(Boolean.TRUE.equals(result.body().get(RoomLifecycleAuthority.TERMINAL_FIELD))){String reason=String.valueOf(result.body().getOrDefault(RoomLifecycleAuthority.TERMINAL_REASON_FIELD,"ROOM_DISSOLVED"));try{lifecycleCompletion.get().complete(roomId,request.requestId(),request.traceId(),reason);}catch(RuntimeException failure){System.err.println("room lifecycle completion deferred roomId="+roomId+" cause="+failure.getMessage());}}
        if(Boolean.TRUE.equals(result.body().get(RoomMembershipLifecycle.MEMBER_LEFT_FIELD))){long accountId=Long.parseLong(String.valueOf(result.body().get(RoomMembershipLifecycle.MEMBER_LEFT_ACCOUNT_ID_FIELD)));if(accountId!=Long.parseLong(session.userId()))throw new SecurityException("room membership lifecycle identity mismatch");membershipCompletion.get().left(roomId,accountId,request.requestId(),request.traceId());}
        return java.util.List.of();
    }
    private static final java.util.regex.Pattern POKER_REGION_DISPATCH =
            java.util.regex.Pattern.compile("^poker\\.([A-Z]{2}[1-5][0-9]{2})\\.dispatch$");
    /**
     * Region-scoped room snapshot id is taken from the generated protocol registry: a region
     * uses `poker.&lt;CODE&gt;.state_push` only when the machine source declares both its dispatch and
     * its region snapshot; every other room command keeps `common.room.state_push`. New regions
     * therefore need no gateway change.
     */
    private static String statePushId(String msgId){
        if(msgId==null)return "common.room.state_push";
        java.util.regex.Matcher region=POKER_REGION_DISPATCH.matcher(msgId);
        if(!region.matches()||!GeneratedProtocolIds.DEFINITIONS.containsKey(msgId))return "common.room.state_push";
        String regionPush="poker."+region.group(1)+".state_push";
        return GeneratedProtocolIds.DEFINITIONS.containsKey(regionPush)?regionPush:"common.room.state_push";
    }
    private static String socialEvent(WebSocketFrame request){
        if(!"common.room.dispatch".equals(request.msgId()))return null;
        return switch(String.valueOf(request.body().get("action"))){
            case "quick_text" -> "room.quick_text";
            case "magic_expression" -> "room.magic_expression";
            case "voice" -> "room.voice";
            default -> null;
        };
    }
    public void configureLifecycleCompletion(LifecycleCompletion completion){if(!lifecycleCompletion.compareAndSet(lifecycleCompletion.get(),java.util.Objects.requireNonNull(completion)))throw new IllegalStateException("room lifecycle completion already configured");}
    public void configureMembershipCompletion(MembershipCompletion completion){if(!membershipCompletion.compareAndSet(membershipCompletion.get(),java.util.Objects.requireNonNull(completion)))throw new IllegalStateException("room membership completion already configured");}
    public void configurePresenceSink(PresenceSink sink){if(!presenceSink.compareAndSet(presenceSink.get(),java.util.Objects.requireNonNull(sink)))throw new IllegalStateException("room presence sink already configured");}
    public void publishAuthoritativeState(long roomId,String requestId,String traceId,long sequence){for(Binding binding:bindings.values())if(binding.roomId()==roomId&&binding.context().channel().isActive())try{Perspective perspective=views.view(roomId,binding.playerId());if(!binding.playVersion().equals(perspective.playVersion()))continue;Map<String,Object>event=new LinkedHashMap<>();event.put("protocolVersion","2.0");event.put("msgId","common.room.state_push");event.put("kind","push");event.put("requestId",requestId);event.put("seq",Math.max(1L,sequence));event.put("timestamp",clock.millis());event.put("traceId",traceId);event.put("playVersion",perspective.playVersion());event.put("body",perspective.body());binding.context().writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(event)));}catch(Exception failure){System.err.println("room lifecycle broadcast failed roomId="+roomId+" playerId="+binding.playerId()+" cause="+failure.getMessage());}}
    public void publishWaitingRoomExpired(long roomId,String requestId,String traceId){for(Binding binding:bindings.values())if(binding.roomId()==roomId&&binding.context().channel().isActive())try{Map<String,Object>payload=Map.of("roomId",roomId,"reasonCode","WAITING_ROOM_EXPIRED","message","房间超过2小时未开始，已自动解散");Map<String,Object>event=GatewayEnvelopeFactory.push("common.room.auto_dissolved",requestId,1L,traceId,"common.room.auto_dissolved",payload,clock);binding.context().writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(event)));}catch(Exception failure){System.err.println("room expiration broadcast failed roomId="+roomId+" playerId="+binding.playerId()+" cause="+failure.getMessage());}}
    public void publishInactiveRoomExpired(long roomId,String requestId,String traceId){for(Binding binding:bindings.values())if(binding.roomId()==roomId&&binding.context().channel().isActive())try{Map<String,Object>payload=Map.of("roomId",roomId,"reasonCode","ROOM_INACTIVITY_EXPIRED","message","房间已12小时无任何游戏操作，已自动解散");Map<String,Object>event=GatewayEnvelopeFactory.push("common.room.auto_dissolved",requestId,1L,traceId,"common.room.auto_dissolved",payload,clock);binding.context().writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(event)));}catch(Exception failure){System.err.println("inactive room expiration broadcast failed roomId="+roomId+" playerId="+binding.playerId()+" cause="+failure.getMessage());}}
    public int connectionCount(){return bindings.size();}
}
