package com.ddm.server.common.rocketmq;

import BaseCommon.CommLog;
import com.google.gson.Gson;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * @author xsj
 * @date 2020/8/12 15:32
 * @description mq生产者管理类
 */
public class MqProducerMgr {
    public enum Confirmation { CONFIRMED, REJECTED, UNKNOWN, UNAVAILABLE }
    private String namesrvAddr;
    private String groupName;
    private Integer retryTimesWhenSendAsyncFailed;
    private MQProducer mqProducer;

    private MqProducerMgr() {

    }

    public static MqProducerMgr get() {
        return SingleCase.INSTANCE;
    }

    public void loadConfig(String path) throws Exception {
        try (InputStream input = new FileInputStream(path)) {
            loadConfig(input);
        }
        mqProducer = mqProducer();
    }

    private void loadConfig(InputStream in) throws Exception {
        Properties pro = new Properties();
        pro.load(in);
        this.retryTimesWhenSendAsyncFailed = Integer.parseInt(pro.getProperty("mq.retry.times"));
        this.namesrvAddr = pro.getProperty("mq.namesrv.addr");
        this.groupName = pro.getProperty("mq.group.name");
    }

    private DefaultMQProducer mqProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(groupName);
        // 设置NameServer的地址
        producer.setNamesrvAddr(namesrvAddr);
        // 启动Producer实例
        try {
            producer.start();
        } catch (MQClientException e) {
            CommLog.error(e.getMessage(), e);
        }
        producer.setRetryTimesWhenSendAsyncFailed(retryTimesWhenSendAsyncFailed);
        return producer;
    }

    /**
     * 发送到数据到MQ
     *
     * @param topic 主题名
     * @param body  消息内容
     */
    public void send(String topic, Object body) {
        Confirmation result=sendConfirmed(topic,body);
        if (result!=Confirmation.CONFIRMED) CommLog.error("RocketMQ send not confirmed topic="+topic+", result="+result);
    }

    /** Synchronous acknowledgement with a small bounded retry; callers may fail closed. */
    public Confirmation sendConfirmed(String topic, Object body) {
        return sendConfirmed(topic,stableKey(topic,body),body);
    }

    public Confirmation sendConfirmed(String topic,String businessKey,Object body) {
        if(topic==null||topic.isBlank()||businessKey==null||businessKey.isBlank())throw new IllegalArgumentException("topic and businessKey are required");
        if(mqProducer==null)return Confirmation.UNAVAILABLE;
        boolean dispatchAttempted=false;
        try {
            byte[] bytes;
            if (body != null) {
                MqAbsBo mqAbsBo = (MqAbsBo) body;
                mqAbsBo.setClazzName(body.getClass().getName());
                bytes = new Gson().toJson(body).getBytes("UTF-8");
            } else {
                bytes = null;
            }
            Message msg = new Message(topic, "tagTest", businessKey, bytes);
            dispatchAttempted=true;
            SendResult result=mqProducer.send(msg);
            return classify(result);
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
            // Synchronous send exceptions may occur after broker acceptance.  Do not
            // blindly retry an unknown outcome; caller/outbox owns reconciliation.
            return classifyFailure(dispatchAttempted);
        }
    }

    public static Confirmation classify(SendResult result){
        if(result==null)return Confirmation.UNKNOWN;
        return result.getSendStatus()==SendStatus.SEND_OK?Confirmation.CONFIRMED:Confirmation.REJECTED;
    }
    public static Confirmation classifyFailure(boolean dispatchAttempted){return dispatchAttempted?Confirmation.UNKNOWN:Confirmation.UNAVAILABLE;}
    private static String stableKey(String topic,Object body){
        String raw=topic+":"+new Gson().toJson(body);
        try{return "aoo-"+java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception impossible){throw new IllegalStateException("SHA-256 unavailable",impossible);}
    }

    /**
     * 发送到数据到MQ
     *
     * @param topic 主题名
     * @param body  消息内容
     */
    public void sendGateway(String topic, Object body) {
        try {
            byte[] bytes;
            if (body != null) {
                bytes = (byte[]) body;
            } else {
                bytes = null;
            }
            Message msg = new Message(topic, "tagTest", "keyTest", bytes);
            mqProducer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                }

                @Override
                public void onException(Throwable e) {
                    CommLog.error(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            CommLog.error(e.getMessage(), e);
        }
    }


    /**
     * 创建主题
     *
     * @param topic
     */
    public void createTopic(String topic) {
        try {
            CommLog.info("RocketMQ topic create: {}", topic);
            mqProducer.createTopic("TBW102", topic, 4, java.util.Collections.emptyMap());
        } catch (Exception e) {
            CommLog.error("RocketMQ topic create failed: " + topic, e);
        }
    }

    private static class SingleCase {
        public static final MqProducerMgr INSTANCE = new MqProducerMgr();
    }

}
