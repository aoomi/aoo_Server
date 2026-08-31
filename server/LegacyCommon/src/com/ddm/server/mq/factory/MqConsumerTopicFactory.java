package com.ddm.server.mq.factory;


import BaseCommon.CommLog;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.RedisSource;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.CollectionUtils;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.websocket.def.MessageType;
import com.ddm.server.websocket.def.SubscribeEnum;
import com.ddm.server.websocket.message.MessageToServerHead;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * 消费的的订阅分类
 */
@Data
public class MqConsumerTopicFactory {

    private static class SingletonHolder {
        static final MqConsumerTopicFactory instance = new MqConsumerTopicFactory();
    }

    private MqConsumerTopicFactory() {
    }

    public static MqConsumerTopicFactory getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 连接配置
     */
    private Map<SubscribeEnum, ConcurrentSkipListSet<String>> connectMap = Maps.newConcurrentMap();

    /**
     * 通知记录列表
     */
    private Set<String> notifySet = new HashSet<>();

    /**
     * 当前服务端的主题
     */
    private String curConsumerTopic = Config.getLocalServerTopic();

    private ITryConnectNotify iTryConnectNotify;

    public void init(ITryConnectNotify iTryConnectNotify) {
        this.iTryConnectNotify = iTryConnectNotify;
        // 增加网关订阅信息
        SubscribeEnum subscribeEnum = SubscribeEnum.nameOf(Config.ServerIDStr().toUpperCase());
        if (Objects.isNull(subscribeEnum)) {
            CommLog.error("初始化订阅信息失败，服务名称错误：{}, 退出服务器启动", Config.ServerIDStr().toUpperCase());
            System.exit(-1);
            return;
        }
        // 记录本地订阅
        RedisSource.getSetV(subscribeEnum.name()).add(this.curConsumerTopic);
        // 获取其他已存在的订阅主题列表
        this.addConnect(SubscribeEnum.GATE, RedisSource.getSetV(SubscribeEnum.GATE.name()).value());
        // 尝试通知所有订阅接口
        this.value().forEach(k -> tryConnectNotify(k));
    }


    /**
     * 尝试连接通知
     *
     * @param topic
     */
    public void tryConnectNotify(String topic) {
        if (this.notifySet.contains(topic)) {
            return;
        }
        MessageToServerHead newMessageHead = new MessageToServerHead();
        newMessageHead.setMessageId((short) MessageType.Notify.ordinal());
        newMessageHead.setServerId((short) Config.ServerID());
        newMessageHead.setTopic(Config.getLocalServerTopic());
        ByteBuf newByteBuf = InnerMessageCodecFactory.getInstance().gateToGameServerEncode(newMessageHead, null);
        byte[] req = new byte[newByteBuf.readableBytes()];
        newByteBuf.readBytes(req);
        MqProducerMgr.get().sendGateway(topic, req);
        this.iTryConnectNotify.registryNotify();
        this.iTryConnectNotify.serverNotify();
    }

    /**
     * 记录收到通知的主题
     *
     * @param topic
     */
    public void addNotifySet(String topic) {
        this.notifySet.add(topic);
    }

    /**
     * 增加连接
     *
     * @param subscribeEnum
     * @param topic
     */
    public final void addConnect(SubscribeEnum subscribeEnum, String topic) {
        if (this.connectMap.containsKey(subscribeEnum)) {
            this.connectMap.get(subscribeEnum).add(topic);
        } else {
            ConcurrentSkipListSet<String> topicSet = new ConcurrentSkipListSet<>();
            topicSet.add(topic);
            this.connectMap.put(subscribeEnum, topicSet);
        }
    }

    /**
     * 增加连接
     *
     * @param subscribeEnum
     * @param stringSet
     */
    public final void addConnect(SubscribeEnum subscribeEnum, Set<String> stringSet) {
        if (this.connectMap.containsKey(subscribeEnum)) {
            if (CollectionUtils.isNotEmpty(stringSet)) {
                this.connectMap.get(subscribeEnum).addAll(stringSet);
            }
        } else {
            ConcurrentSkipListSet<String> topicSet = new ConcurrentSkipListSet<>();
            if (CollectionUtils.isNotEmpty(stringSet)) {
                topicSet.addAll(stringSet);
            }
            this.connectMap.put(subscribeEnum, topicSet);
        }
    }

    /**
     * 移除连接
     *
     * @param subscribeEnum
     * @param topic
     */
    public final void removeConnect(SubscribeEnum subscribeEnum, String topic) {
        if (this.connectMap.containsKey(subscribeEnum)) {
            this.connectMap.get(subscribeEnum).remove(topic);
            this.notifySet.remove(topic);
        }
    }


    /**
     * 获取一个指定服务端的发布主题
     *
     * @param subscribeEnum 订阅服务度
     * @return
     */
    public String getPubTopic(SubscribeEnum subscribeEnum) {
        if (this.connectMap.containsKey(subscribeEnum)) {
            Set<String> stringSet = this.connectMap.get(subscribeEnum);
            int rn = CommMath.randomInt(stringSet.size());
            int i = 0;
            for (String e : stringSet) {
                if (i == rn) {
                    return e;
                }
                i++;
            }
            return stringSet.iterator().next();
        }
        return "";
    }


    /**
     * @param topic
     * @return
     */
    public String getPubTopic2ServerName(String topic) {
        return this.connectMap.entrySet().stream().filter(k -> k.getValue().stream().anyMatch(v -> v.startsWith(topic))).map(k -> k.getKey().name()).findFirst().orElse("");
    }

    public List<String> value() {
        return this.connectMap.values().stream().flatMap(k -> k.stream()).collect(Collectors.toList());
    }

    public final void stopCurConnect() {
        this.iTryConnectNotify.stopCurConnect();
    }
}


