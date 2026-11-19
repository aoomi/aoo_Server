package cenum;

/*
 * 玩家身上的宏定义
 * @author zaf
 * */
public class PlayerEnum {

    public static enum GMLEVEL {
        GMLEVEL_NOMAL(0), // 没有GM权限
        GMLEVEL_LEVEL_ONE(1), // gm权限一级 (测试用的)
        GMLEVEL_LEVEL_TWO(2),// gm权限二级 (踢人用 测试用的)
        ;
        private int value;

        private GMLEVEL(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static GMLEVEL valueOf(int value) {
            for (GMLEVEL flow : GMLEVEL.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return GMLEVEL.GMLEVEL_NOMAL;
        }
    }

    /**
     * 试玩用户
     *
     * @author Administrator
     */
    public static enum TRY_TO_PLAY_USERS {
        NEW_USER(0), // 新用户
        CUR_TRIAL_PLAY(1), // 当天试玩
        NOT_TRY_TO_PLAY(2),// 不是试玩
        ;
        private int value;

        private TRY_TO_PLAY_USERS(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static TRY_TO_PLAY_USERS valueOf(int value) {
            for (TRY_TO_PLAY_USERS flow : TRY_TO_PLAY_USERS.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return TRY_TO_PLAY_USERS.NEW_USER;
        }
    }

    /**
     * 活跃初始分
     *
     * @author Administrator
     */
    public static enum ACTIVE_INT {
        EXP_0(0),//0
        EXP_10(10),//10
        EXP_20(20),
        EXP_50(50),
        EXP_100(100),
        EXP_200(200),
        ;
        private int value;

        private ACTIVE_INT(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static ACTIVE_INT valueOf(int value) {
            for (ACTIVE_INT flow : ACTIVE_INT.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return ACTIVE_INT.EXP_0;
        }
    }


}
