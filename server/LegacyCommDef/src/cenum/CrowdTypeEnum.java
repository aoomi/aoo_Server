package cenum;

/**
 * 1：全体玩家、2：代理及名下玩家、3：除代理及名下玩家外其他玩家
 *
 * @author Administrator
 */
public enum CrowdTypeEnum {
    NOT(0),
    /**
     * 全体玩家
     */
    ALL_PLAYER(1),
    /**
     * 代理及名下玩家
     */
    AGENTS_AND_PLAYER(2),
    /**
     * 除代理及名下玩家外其他玩家
     */
    NOT_AGENTS_AND_PLAYER(3),
    /**
     * 指定亲友圈
     */
    CLUB(4),
    /**
     * 指定赛事
     */
    UNION(5),
    /**
     * 指定城市
     */
    CITY(6),
    ;

    private int value;

    CrowdTypeEnum(int value) {
        this.value = value;
    }

    // 获取值
    public int value() {
        return this.value;
    }

    public static CrowdTypeEnum valueOf(int value) {
        for (CrowdTypeEnum flow : CrowdTypeEnum.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return CrowdTypeEnum.NOT;
    }
}
