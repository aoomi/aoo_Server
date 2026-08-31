package business.rocketmq.common;

import com.ddm.server.common.utils.CommLog;

/** Topic names shared with game services. */
public final class RocketmqConstant {
    public static final String PLAYER_CHANGE_NOTIFY = "PLAYER_CHANGE_NOTIFY";
    public static final String PLAYER_UPDATE_INFO_NOTIFY = "PLAYER_UPDATE_INFO_NOTIFY";
    public static final String PLAYER_ALONE_LOGIN_NOTIFY = "PLAYER_ALONE_LOGIN_NOTIFY";
    public static final String ALL_PLAYER_TEST_NOTIFY = "ALL_PLAYER_TEST_NOTIFY";

    private RocketmqConstant() {}

    /**
     * The legacy implementation emitted synthetic messages at startup through a
     * Boot-3-only starter. Production producers now use the core RocketMQ client
     * at the actual business call sites; startup no longer mutates message state.
     */
    public static void init() {
        CommLog.info("RocketMQ topics initialized without synthetic startup messages");
    }
}
