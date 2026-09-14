/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.network.client2game;

import BaseCommon.CommLog;
import business.player.Player;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.NetUtil;
import com.ddm.server.mq.factory.InnerMessageCodecFactory;
import com.ddm.server.websocket.codecfactory.MessageEncoder;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.def.MessageType;
import com.ddm.server.websocket.def.TerminalType;
import com.ddm.server.websocket.message.MessageToServerHead;
import com.ddm.server.websocket.server.ServerSession;
import core.server.ServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import jsproto.c2s.iclass.S0110_KickOut;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.session.IoSession;

import java.util.Objects;
import java.net.InetSocketAddress;

/**
 * 客户端连接代理对象,用于连接账号信息和连接句柄
 *
 * @author Clark
 */

/**
 * @author Deniulor
 */
public class ClientSession extends ServerSession {

    private Player player;
    private int playerSid;
    private String accessToken;
    private int encryptedkey;
    private boolean valid;
    private boolean isPrint = false;
    private String remoteAddress;
    private String wxUnionid;//微信ID
    private String token;
    private long phone;

    /**
     * mina的session
     *
     * @param session
     * @param sessionID
     */
    public ClientSession(IoSession session, long sessionID) {
        super(TerminalType.GameServer, ServerConfig.ServerID(), TerminalType.Client, session, sessionID);
        this.valid = false;
        this.encryptedkey = (int) sessionID;
        remoteAddress = session.getRemoteAddress().toString();
    }

    /**
     * netty的session
     *
     * @param session
     * @param sessionID
     */
    public ClientSession(Channel session, long sessionID) {
        super(TerminalType.GameServer, ServerConfig.ServerID(), TerminalType.Client, session, sessionID);
        this.valid = false;
        this.encryptedkey = (int) sessionID;
        remoteAddress = session.remoteAddress().toString();
        // 直连 WebSocket 不经过 Gateway，resetSession() 不会为会话填充来源 IP。
        // 从 Netty channel 补齐，避免首次创建角色时把空字符串按 IPv4 解析。
        if (session.remoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) session.remoteAddress();
            this.set_remoteIP(address.getAddress().getHostAddress());
        }
    }

    @Override
    public String toString() {
        return "ClientSession{" +
                "player=" + player +
                ", accountID=" + super.getAccountID() +
                ", playerSid=" + playerSid +
                ", accessToken='" + accessToken + '\'' +
                ", encryptedkey=" + encryptedkey +
                ", valid=" + valid +
                ", isPrint=" + isPrint +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", wxUnionid='" + wxUnionid + '\'' +
                ", session=" + session +
                ", sessionID=" + sessionID +
                ", _remoteIP='" + _remoteIP + '\'' +
                ", _remotePort=" + _remotePort +
                '}';
    }

    public Player getPlayer() {
        return player;
    }

    public void bindPlayer(Player player) {
        this.player = player;
        if (Objects.nonNull(this.player)) {
            this.setPid(this.player.getPid());
            if (NetUtil.isIPv4Private(this.remoteIP())) {
                CommLog.error("bindPlayer set ip pid;{},ip:{}", this.player.getPid(), this.remoteIP());
                return;
            }
            this.player.setIp(remoteIP());
        }
    }

    public void losePlayer() {
        this.player = null;
        S0110_KickOut msg = S0110_KickOut.make(2);
        this.notifyMessage("kickout", msg);
        close();
    }

    public void kickOutPlayer() {
        S0110_KickOut msg = S0110_KickOut.make(2);
        this.notifyMessage("kickout", msg);
    }

    public void uuidLosePlayer() {
        this.player = null;
        close();
    }

    public boolean isPrint() {
        return isPrint;
    }

    public void setPrint(boolean isPrint) {
        this.isPrint = isPrint;
    }

    public void setAccessToken(String access_token) {
        this.accessToken = access_token;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public int getEncryptedkey() {
        return encryptedkey;
    }

    public void setEncryptedkey(int encryptedkey) {
        this.encryptedkey = encryptedkey;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean isValid) {
        this.valid = isValid;
    }


    public int getPlayerSid() {
        return playerSid;
    }

    public void setPlayerSid(int playerSid) {
        this.playerSid = playerSid;
    }

    @Override
    public void onCreated() {
    }

    @Override
    public void onClosed() {
        // 断开链接
        if (this.player != null) {
            player.loseSession();
        }

    }


    public long getPhone() {
        return phone;
    }

    public void setPhone(long phone) {
        this.phone = phone;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }


    public String getWxUnionid() {
        return wxUnionid;
    }

    public void setWxUnionid(String wxUnionid) {
        if (StringUtils.isEmpty(wxUnionid)) {
            return;
        }
        this.wxUnionid = "wx_" + wxUnionid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }


    @Override
    public void sendToServer(ByteBuf buf, String topic) {
        CommLog.info("sendToServer accountId:{}, topic:{}", this.getAccountID(), topic);
        // 本地网页预览直接连接游戏服时没有独立 Gateway，响应直接写回当前 WebSocket。
        // 正式集群连接会带 gatewaySessionId，仍按原逻辑通过 MQ 返回 Gateway。
        if (this.getChannel() != null && this.getGatewaySessionId() == 0) {
            this.getChannel().pipeline().context(MessageEncoder.class)
                    .writeAndFlush(new BinaryWebSocketFrame(buf));
            return;
        }
        if (ErrorCode.Not.name().equals(this.getPubTopic())) {
            return;
        }
        MessageToServerHead messageHead = new MessageToServerHead();
        messageHead.setAccountId(this.getAccountID());
        messageHead.setSessoinId(this.getGatewaySessionId());
        messageHead.setPid(this.getPid());
        messageHead.setMessageId((short) MessageType.Response.ordinal());
        messageHead.setServerId((short) Config.ServerID());
        messageHead.setValid((byte) (this.isValid() ? 1 : 0));
        messageHead.setTopic(StringUtils.isEmpty(topic) ? this.getSubjectTopic() : topic);
        ByteBuf byteBuf = InnerMessageCodecFactory.getInstance().gateToGameServerEncode(messageHead, buf);
        byte[] req = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(req);
        MqProducerMgr.get().sendGateway(this.getPubTopic(), req);
        CommLog.info("sendToServer accountId:{},topic:{},PubTopic:{}", this.getAccountID(), topic, this.getPubTopic());
        if (StringUtils.isNotEmpty(topic)) {
            this.setCurPubTopic(ErrorCode.Not.name());
        }
    }
}
