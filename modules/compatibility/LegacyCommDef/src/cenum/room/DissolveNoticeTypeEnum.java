package cenum.room;

/**
 * 解散通知类型
 */
public enum DissolveNoticeTypeEnum {
    /**
     * 0：默认
     */
    DEFAULT,
    /**
     * 1：玩家名称-竞技点低于设置值，房间已自动解散
     */
    UNION_SPORTS_POINT_LOW,
    /**
     * 修改房间配置
     */
    CHANGE_ROOMCRG,

    /**
     * 俱乐部、赛事
     * 管理员、盟主或圈主解散
     */
    SPECIAL_DISSOLVE,
    ;
}
