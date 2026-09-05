package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks authenticated sockets so Account can atomically replace every older login generation. */
public final class GatewaySessionRegistry {
    public static final int SESSION_REPLACED_CLOSE_CODE=4001;
    private static final GatewaySessionRegistry GLOBAL=new GatewaySessionRegistry();
    private final Map<Channel,ConnectionIdentity> connections=new ConcurrentHashMap<>();
    public static GatewaySessionRegistry global(){return GLOBAL;}
    /**
     * 一个页面在大厅到房间切换期间可短暂拥有多个 transport；只有不同 pageInstanceId 才代表其他页面。
     */
    public synchronized int connected(Channel channel,ConnectionIdentity identity,ObjectMapper json,Clock clock){
        connections.put(channel,identity);
        return closeOtherPageGroups(identity.userId(),identity.pageInstanceId(),channel,json,clock);
    }
    public void disconnected(Channel channel){connections.remove(channel);}
    public synchronized int replace(long accountId,String currentSessionId,long currentGeneration,ObjectMapper json,Clock clock){
        return closeOtherPageGroups(accountId,null,null,json,clock);
    }
    private int closeOtherPageGroups(long accountId,String retainedPage,Channel retained,ObjectMapper json,Clock clock){
        int closed=0;
        for(Map.Entry<Channel,ConnectionIdentity> binding:connections.entrySet()){
            Channel channel=binding.getKey();ConnectionIdentity identity=binding.getValue();
            if(identity.userId()!=accountId||channel==retained||(retainedPage!=null&&retainedPage.equals(identity.pageInstanceId())))continue;
            String requestId=UUID.randomUUID().toString();
            try{
                var payload=Map.of("reasonCode","SESSION_REPLACED","message","你的账号已在其他设备登录");
                var event=GatewayEnvelopeFactory.push("system.kick_out",requestId,1L,requestId,
                        "system.kick_out",payload,clock);
                // 关闭帧必须排在文本写成功之后，否则浏览器可能先观察到 close，永远收不到顶号原因。
                channel.writeAndFlush(new TextWebSocketFrame(json.writeValueAsString(event))).addListener(write->{
                    if(!write.isSuccess()){channel.close();return;}
                    channel.writeAndFlush(new CloseWebSocketFrame(SESSION_REPLACED_CLOSE_CODE,"SESSION_REPLACED"))
                            .addListener(ChannelFutureListener.CLOSE);
                });
                closed++;
            }catch(Exception failure){channel.close();}
        }
        return closed;
    }
}
