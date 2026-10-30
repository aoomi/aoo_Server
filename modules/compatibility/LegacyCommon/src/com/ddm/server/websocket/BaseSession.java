/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.websocket;

import BaseCommon.CommLog;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import com.ddm.server.netty.SessionConnectMgr;
import com.ddm.server.websocket.def.SubscribeEnum;
import com.ddm.server.websocket.handler.MessageHeader;
import com.ddm.server.websocket.message.MessageToServerHead;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.session.IoSession;

import java.net.SocketAddress;
import java.util.Objects;


/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
@Data
public abstract class BaseSession {

    //mina的管道
    protected IoSession session;
    //netty的管道
    protected Channel channel;
    protected long sessionID;
    protected String _remoteIP = "";
    protected int _remotePort;
    private PkgCacheMgr pkgCacheMgr = new PkgCacheMgr();
    private long accountID;
    private long pid;
    /**
     * 当前订阅主题
     */
    private String curSubjectTopic = "";

    /**
     * 当前发布主题
     */
    private String curPubTopic = "";

    private long gatewaySessionId = 0;

    private int curSequence = 0;

    /**
     * 默认走大厅
     */
//    private SubscribeEnum subscribeEnum = SubscribeEnum.HALL;
    public BaseSession(IoSession session, long sessionID) {
        this.session = session;
        this.sessionID = sessionID;
        CommLogD.info("create session id:{} from ip:{}", getSessionId(), remoteIP());
    }

    public BaseSession(Channel session, long sessionID) {
        this.channel = session;
        this.sessionID = sessionID;
        CommLogD.info("create session id:{} from ip:{}", getSessionId(), remoteIP());
    }

    public void close() {
        CommLogD.info("close session id:{} from ip:{}", getSessionId(), remoteIP());
        if (channel != null) {
            SessionConnectMgr.getInstance().closeChannel(channel);
            channel.close();
        } else {
            session.close(true);
        }
    }

    /**
     * 获取发布主题
     *
     * @return
     */
    public String getPubTopic() {
        if (StringUtils.isEmpty(this.getCurPubTopic())) {
            // 新的session 连接默认都走大厅
            this.setCurPubTopic(MqConsumerTopicFactory.getInstance().getPubTopic(SubscribeEnum.GATE));
        }
        return this.getCurPubTopic();
    }

    /**
     * 获取订阅主题
     *
     * @return
     */
    public String getSubjectTopic() {
        if (StringUtils.isEmpty(this.getCurSubjectTopic())) {
            // 新的session 连接默认都走大厅
            this.setCurSubjectTopic(Config.getLocalServerTopic());
        }
        return this.getCurSubjectTopic();
    }

    public long getSessionId() {
        return sessionID;
    }

    public PkgCacheMgr getPkgCacheMgr() {
        return pkgCacheMgr;
    }

    public SocketAddress remoteAddress() {
        if (channel != null) {
            return channel.remoteAddress();
        }
        return session.getRemoteAddress();
    }

    public SocketAddress localAddress() {
        if (channel != null) {
            return channel.localAddress();
        }
        return session.getLocalAddress();
    }

    public boolean isConnected() {
        if (channel != null) {
            return channel.isOpen() && channel.isActive();
        }
        return session.isConnected() && !session.isClosing();
    }

    public String remoteIP() {
        if (Objects.nonNull(this.channel)) {
            return this._remoteIP;
        }
        return (String) session.getAttribute("KEY_SESSION_CLIENT_IP");
    }

    public void resetRemoteIP() {
        CommLogD.info("resetRemoteIP session id:{} from ip:{}", getSessionId(), remoteIP());
    }


    public int remotePort() {
        try {
            if (this._remotePort == 0) {
                String remoteAddr = remoteAddress().toString().replaceFirst("/", "");
                int indexOfFound = remoteAddr.indexOf(':');
                if (indexOfFound >= 0) {
                    String strPort = remoteAddr.substring(indexOfFound + 1, remoteAddr.length());
                    this._remotePort = Integer.parseInt(strPort);
                }
            }
            return this._remotePort;
        } catch (Throwable e) {
            CommLogD.error("remotePort:() session not connected!");
            return 0;
        }
    }

    public void onSent(String message) {
        CommLogD.debug("session {} sent {} chars", this.hashCode(), message.length());
    }

    public void onReceived(MessageHeader header, String message) {
        CommLogD.debug("session {} received ({}), {} bytes", this.hashCode(), header.event, message.length());
    }

    public abstract void onCreated();

    public abstract void onClosed();


    public IoSession getSession() {
        return session;
    }


    public abstract void sendToServer(ByteBuf buf, String value);


    public void resetSession(MessageToServerHead messageHead) {
        this.setAccountID(messageHead.getAccountId());
        this.set_remoteIP(messageHead.getIp());
        this.setPid(messageHead.getPid());
        this.setGatewaySessionId(messageHead.getSessoinId());
        this.setCurPubTopic(messageHead.getTopic());
    }

    public boolean checkSequenceException(short sequence) {
        int unsignedSequence = Short.toUnsignedInt(sequence);
        if (this.curSequence == unsignedSequence) {
            // 重复提交
            CommLog.error("checkSequenceException = sessionid:{},curSequence:{},sequence:{}", this.getSessionId(), this.curSequence, sequence);
            return false;
        } else if (this.curSequence < unsignedSequence) {
            this.curSequence = unsignedSequence;
            return true;
        } else {
            if (this.curSequence >= 65_000 && unsignedSequence <= 100) {
                CommLog.error("checkSequenceException > 5000 sessionid:{},curSequence:{},sequence:{}", this.getSessionId(), this.curSequence, sequence);
                this.curSequence = unsignedSequence;
            } else {
                CommLog.error("checkSequenceException error sessionid:{},curSequence:{},sequence:{}", this.getSessionId(), this.curSequence, sequence);
            }
            return this.curSequence == unsignedSequence;
        }
    }
}
