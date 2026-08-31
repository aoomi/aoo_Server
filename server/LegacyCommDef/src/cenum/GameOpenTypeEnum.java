package cenum;

public enum GameOpenTypeEnum {
    /**
     * 所有
     */
    GAME_ALL,
    /**
     * 城市
     */
    GAME_CITY,
    /**
     * 赛事
     */
    GAME_UNION,
    ;

    public static GameOpenTypeEnum valueOf(int ordinal) {
        for (GameOpenTypeEnum flow : GameOpenTypeEnum.values()) {
            if (flow.ordinal() == ordinal) {
                return flow;
            }
        }
        return null;
    }
}
