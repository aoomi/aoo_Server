package com.ddm.server.netty;

import com.ddm.server.websocket.BaseSession;
import com.ddm.server.websocket.IMessageDispatcher;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.Data;

import java.util.Map;
import java.util.Objects;

@Data
public class SessionConnectMgr<Session extends BaseSession> {

    /**
     * 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
     */
    private static class SingletonHolder {
        /**
         * 静态初始化器，由JVM来保证线程安全
         */
        private static SessionConnectMgr instance = new SessionConnectMgr();
    }

    /**
     * 私有化构造方法
     */
    private SessionConnectMgr() {
    }


    /**
     * 获取单例
     *
     * @return
     */
    public static SessionConnectMgr getInstance() {
        return SingletonHolder.instance;
    }

    protected IMessageDispatcher<Session> messageDispatcher;

    /**
     * 解密密钥
     */
    private final static String DECRYPT_KEY = "decryptKey";

    /**
     * 验证值
     */
    private final static String VALIDATION_VALUE = "validationValue";

    /**
     * 账号id
     */
    private final static String ACCOUNT_ID = "accountID";

    /**
     * 令牌
     */
    private final static String TOKEN = "Token";

    /**
     * 标识（1:验证账号服token,2:验证服务端token）
     */
    private final static String SIGN = "Sign";

    /**
     * 账号服的解密秘钥值
     */
    private String accountServerDecryptKey = "A599vnI9Hjge2Pl4";


    /**
     * 连接map<sessionID,session>
     */
    protected final Map<Long, Session> connections = Maps.newConcurrentMap();

    /**
     * accountId->sessionId
     */
    protected final Map<Long, Long> aidToSessionIdMap = Maps.newConcurrentMap();

    /**
     * chanel的sessionKey
     */
    protected AttributeKey<Long> sessionAttr = AttributeKey.valueOf("sessionID");

    /**
     * 尝试心跳次数
     */
    private int tryHeartBeatLimit;


    /**
     * 通过sessionID获取session
     *
     * @param sessionId
     * @return
     */
    public Session getSession(long sessionId) {
        return connections.get(sessionId);
    }

    /**
     * 通过sessionID移除session
     *
     * @param sessionId
     * @return
     */
    public Session removeSession(long sessionId) {
        return connections.remove(sessionId);
    }

    /**
     * 映射
     *
     * @param sessionId
     * @param session
     */
    public void connectionsPut(long sessionId, Session session) {
        this.connections.put(sessionId, session);
    }

    public IMessageDispatcher<Session> getMessageDispatcher() {
        return messageDispatcher;
    }

    public void setMessageDispatcher(IMessageDispatcher<Session> messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    /**
     * 获取对应的session信息
     *
     * @param channel 通道
     * @return
     */
    public Session getSession(Channel channel) {
        if (Objects.isNull(channel)) {
            return null;
        }
        long sessionId = channel.attr(this.sessionAttr).get();
        return this.getSession(sessionId);
    }

    /**
     * 获取对应的sessionId
     *
     * @param channel 通道
     * @return
     */
    public long getSessionId(Channel channel) {
        if (Objects.isNull(channel)) {
            return 0L;
        }
        return channel.attr(this.sessionAttr).get();
    }


    /**
     * 是否存在账号id
     *
     * @param accountId
     * @return
     */
    public boolean existAccountId(long accountId) {
        return this.aidToSessionIdMap.containsKey(accountId);
    }

    public Session getAccountIdToSession(long accountId) {
        if (this.aidToSessionIdMap.containsKey(accountId)) {
            long sessionId = this.aidToSessionIdMap.get(accountId);
            return connections.get(sessionId);
        }
        return null;
    }


    /**
     * 不存在账号id
     *
     * @param accountId
     * @return
     */
    public boolean notExistAccountId(long accountId) {
        return !this.existAccountId(accountId);
    }


    /**
     * 创建一个新的空session
     *
     * @return
     */
    public Session newSession(Session session, long accountId) {
        session.getChannel().attr(SessionConnectMgr.getInstance().getSessionAttr()).setIfAbsent(session.getSessionId());
        this.connectionsPut(session.getSessionId(), session);
        this.aidToSessionIdMap.put(accountId, session.getSessionId());
        return session;
    }


    /**
     * 关闭通道
     */
    public void closeChannel(Channel channel) {
        if (Objects.nonNull(channel)) {
            long sessionId = this.getSessionId(channel);
            BaseSession client = this.removeSession(sessionId);
            if (Objects.nonNull(client)) {
                client.onClosed();
                this.aidToSessionIdMap.remove(client.getAccountID());
            }
            channel.close();
        }
    }
}