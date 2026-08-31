package cenum.redis;

/**
 * redis数据key枚举
 */
public enum RedisBydrKeyEnum {
    /**
     * 服务端节点 IP
     */
    IP_SERVER_NODE_SET("IP:SERVER_NODE:SET"),
    SHARE_NODE_MAP("SHARE:NODE:MAP"),
    SHARE_GAME_NODE_SET("SHARE:GAME_NODE:%d:SET"),
    /**
     * 创建者数据统计
     */
    CLUB_PROMOTION_CREATE("CPL_CREATE:TIME:%s,CID:%d"),
    /**
     * 推广员数据统计
     */
    CLUB_PROMOTION_LEVEL("CPL_PROMOTION:TIME:%s:CID:%d:UPID:%d:UID:%d:LV:%d"),
    /**
     * 推广员数据统计
     */
    CLUB_PROMOTION_LEVEL_PLAYGAMID("CPL_PROMOTIONPLAYGAMID:TIME:%s:CID:%d:UPID:%d:UID:%d:LV:%d:"),
    /**
     * 创建者数据统计（中至颁奖）
     */
    CLUB_PROMOTION_CREATE_ZHONGZHI("CPL_CREATE_ZHONGZHI:TIME:%s,CID:%d"),
    /**
     * 推广员数据统计（中至颁奖）
     */
    CLUB_PROMOTION_LEVEL_PLAYGAMID_ZHONGZHI("CPL_PROMOTIONPLAYGAMID_ZHONGZHI:TIME:%s:CID:%d:UPID:%d:UID:%d:LV:%d:"),
    /**
     * 普通成员数据统计
     */
    CLUB_PROMOTION_GENERAL("CPL_GENERAL:TIME:%s:CID:%d:UPID:%d:UID:%d:LV:%d"),
    /**
     * 测试redis是否可以正常使用
     */
    REDIS_TEST("REDIS_TEST"),

    /**
     * 重复提交订单号
     */
    RECHARGE_REPEAT_ORDER_ID("RECHARGE_ORDER_ID:%s"),

    /**
     * 回放码当前
     */
    SHARE_DATE_PLAY_BACK_KEY("SHARE_DATE_PLAY_BACK_KEY:%s"),

    /**
     * 每日回放码
     */
    DAILY_PLAY_BACK_KEY("DAILY_PLAY_BACK_KEY:%s"),


    /**
     * 账号Id 对应的Token
     */
    AID_2_TOKEN("AID:TOKEN:%d:MAP");

    private final String key;

    RedisBydrKeyEnum(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getKey(Object... objects) {
        return String.format(key, objects);
    }
}
