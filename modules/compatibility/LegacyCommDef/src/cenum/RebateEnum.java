package cenum;

public class RebateEnum {

    public enum RebateType {
        None(0), // 0-
        REBATETYPE_REFFER(1), // 1-推荐人
        REBATETYPE_AGENT(2), // 2-代理
        REBATETYPE_TASK(3),    // 3-任务
        REBATETYPE_SHANGJIN(4),    // 4-赏金
        REBATETYPE_LUCK_DRAW(5),//抽奖
        ;
        private int value;

        private RebateType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static RebateType valueOf(int value) {
            for (RebateType flow : RebateType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return RebateType.None;
        }
    }

    //0:玩家1:代理 判断充值用户的推荐人是代理还是玩家
    public enum RebateFlag {
        REBATEFLAG_PLAYER(0), // 1-玩家
        REBATEFLAG_AGENT(1), // 2-代理

        ;
        private int value;

        private RebateFlag(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static RebateFlag valueOf(int value) {
            for (RebateFlag flow : RebateFlag.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return RebateFlag.REBATEFLAG_PLAYER;
        }
    }
}