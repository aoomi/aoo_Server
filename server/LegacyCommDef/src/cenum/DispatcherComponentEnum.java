package cenum;

/**
 * 调度程序组件枚举
 */


public enum DispatcherComponentEnum {
    /**
     * 玩家事件管理器
     */
    PLAYER(0, 8192),

    /**
     * 联赛、亲友圈事件管理器
     */
    CLUB_UNION(1, 8192),

    /**
     * 联赛、亲友圈创建者充值恢复房间配置通知
     */
    CLUB_UNION_PLAYER_CREATE(1, 8192),
    /**
     * 回放队列
     */
    PLAY_BACK(2, 32768),
    /**
     * 房间内通知操作
     */
    ROOM(3, 1024),

    /**
     * 其他记录操作
     */
    OTHER(4, 1024),

    /**
     * 代理分级设置
     */
    PROMOTION_LEVEL(5, 1024),
    /**
     * 推广员分成
     */
    PROMOTION_SHARE(6, 4096),

    /**
     * 手机绑定
     */
    PHONE(7, 1024),

    /**
     * 维护
     */
    MAINTAIN(8, 1024),
    /**
     * 数据库清空数据
     */
    DB_DELETE(9, 1024),


    /**
     * 联赛保险箱功能关闭
     */
    CASE_CLOSE(10, 4096),
    /**
     * 邀请值
     */
    CLUB_INVITE(11, 1024),
    /**
     * 推广员分成
     */
    PROMOTION_SHARE_REVERT(12, 4096),
    /**
     * 联赛自动审核
     */
    UNION_EXAMINE(13, 4096),
    /**
     * 亲友圈初始化区间分成
     */
    PROMOTION_SECTION_INIT(14, 1024),
    /**
     * 成员区间分成修改
     */
    PROMOTION_SECTION_CHANGE(15, 4096),
    /**
     * 本地成员缓存修改
     */
    LOCAL_CLUB_MEMBER(16, 4096),
    /**
     * 本地房间缓存修改
     */
    LOCAL_ROOM(17, 4096),
    /**
     * 本地玩家缓存修改
     */
    LOCAL_PLAYER(18, 4096),
    /**
     * 推广员分成每日一表记录
     */
    PROMOTION_POINT_RECORD(19, 1024),
    /**
     * 身份证认证事件
     */
    ID_CARD(20, 1024),
    ;

    private int value;

    private int bufferSize;

    DispatcherComponentEnum(int value, int bufferSize) {
        this.value = value;
        this.bufferSize = bufferSize;
    }

    public int id() {
        return this.value;
    }

    public int bufferSize() {
        return bufferSize;
    }
}
