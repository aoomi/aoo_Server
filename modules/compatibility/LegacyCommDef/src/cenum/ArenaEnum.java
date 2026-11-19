package cenum;

public class ArenaEnum {

    public enum TimeTypeEnum {
        /**
         * 无限制
         */
        Normal(1),
        /**
         * 每日
         */
        Everyday(2),
        /**
         * 每周
         */
        Weekly(3),

        ;
        private int value;

        private TimeTypeEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

    }

    /**
     * 开始游戏 类型
     *
     * @author Huaxing
     */
    public enum StartGameEnum {
        /**
         * 空状态
         */
        Not(0),
        /**
         * 初始
         */
        InitSet(1),
        /**
         * 继续
         */
        Continue(2),

        ;
        private int value;

        private StartGameEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static StartGameEnum valueOf(int value) {
            for (StartGameEnum flow : StartGameEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return StartGameEnum.Not;
        }
    }

    /**
     * 竞技场 类型
     *
     * @author Huaxing
     */
    public enum ArenaTypeEnum {
        /**
         * 空状态
         */
        Not(0),
        /**
         * 大众
         */
        Public(1),
        /**
         * 专属
         */
        Exclusive(2),

        ;
        private int value;

        private ArenaTypeEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static ArenaTypeEnum valueOf(int value) {
            for (ArenaTypeEnum flow : ArenaTypeEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return ArenaTypeEnum.Not;
        }
    }

    /**
     * 竞技场 比较类型
     *
     * @author Huaxing
     */
    public enum ArenaCompareEnum {
        /**
         * 空状态
         */
        Not(0),
        // 分数,排名
        /**
         * 分数
         */
        POINT(1),
        /**
         * 排名
         */
        RANKING(2),

        ;
        private int value;

        private ArenaCompareEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static ArenaCompareEnum valueOf(int value) {
            for (ArenaCompareEnum flow : ArenaCompareEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return ArenaCompareEnum.Not;
        }
    }

    public enum ArenaStartEnum {
        /**
         * 空状态
         */
        Not(0),
        // 时间,人数
        /**
         * 时间
         */
        TIME(1),
        /**
         * 人数
         */
        NUMBER(2),
        ;
        private int value;

        private ArenaStartEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static ArenaStartEnum valueOf(int value) {
            for (ArenaStartEnum flow : ArenaStartEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return ArenaStartEnum.Not;
        }

    }

    /**
     * 比赛场-赛制
     * <p>
     * 赛制状态1.定局积分赛(默认)2.打立出局赛.3.瑞士移位赛
     *
     * @author Administrator
     */
    public enum FormatTypeEnum {
        /**
         * 空状态
         */
        Not(0),
        /**
         * 定局积分赛
         */
        FixedSet(1),
        /**
         * 打立出局赛
         */
        SetOut(2),
        /**
         * 瑞士移位赛
         */
        ShiftPos(3),
        ;
        private int value;

        private FormatTypeEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static FormatTypeEnum valueOf(int value) {
            for (FormatTypeEnum flow : FormatTypeEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return FormatTypeEnum.Not;
        }

    }

    /**
     * 报名状态
     *
     * @author Huaxing
     */
    public enum EnrollEnum {
        // 报名,预参加,确认参加
        /**
         * 空状态
         */
        Not(0),
        /**
         * 报名
         */
        ENROLL(1),
        /**
         * 预参加
         */
        PRE_JOIN(2),
        /**
         * 确认参加
         */
        CONFIRM_JOIN(3),
        ;
        private int value;

        private EnrollEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 比赛场状态
     *
     * @author Huaxing
     */
    public enum ArenaStateEnum {
        // 等待,开始,结束
        /**
         * 空状态
         */
        Not(0),
        /**
         * 等待
         */
        WAIT(1),
        /**
         * 开始
         */
        START(2),
        /**
         * 结束
         */
        END(3),
        ;
        private int value;

        private ArenaStateEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 比赛场阶段状态
     *
     * @author Huaxing
     */
    public enum ArenaStage {
        // 等待,开始,结束
        /**
         * 空状态
         */
        NOT(0),
        /**
         * 开始
         */
        START(1),
        /**
         * 淘汰
         */
        OUT(2),
        /**
         * 晋级
         */
        PRO(3),
        /**
         * 结束
         */
        END(4),
        /**
         * 排位阶段
         */
        RANKING(5),
        ;
        private int value;

        private ArenaStage(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 时间状态
     *
     * @author Huaxing
     */
    public enum ArenaTimeEnum {
        // 时，分，秒
        /**
         * 空状态
         */
        Not(0),
        /**
         * 小时
         */
        HOUR(1),
        /**
         * 分钟
         */
        MIN(2),
        /**
         * 秒
         */
        SEC(3),
        ;
        private int value;

        private ArenaTimeEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 晋级状态
     *
     * @author Huaxing
     */
    public enum ProStratEnum {
        /**
         * 正常状态
         */
        Not(0),
        /**
         * 成功晋级
         */
        Pro(1),
        /**
         * 淘汰
         */
        Out(2),
        /**
         * 奖励
         */
        Reward(3),

        ;
        private int value;

        private ProStratEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static ProStratEnum valueOf(int value) {
            for (ProStratEnum flow : ProStratEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return ProStratEnum.Not;
        }
    }

    /**
     * 开始时间类型
     *
     * @author Huaxing
     */
    public enum StartTimeTypeEnum {
        /**
         * 正常状态
         */
        Not(0),
        /**
         * 正常
         */
        Normal(1),
        /**
         * 每日
         */
        Everyday(2),
        /**
         * 每周
         */
        Weekly(3),
        ;
        private int value;

        private StartTimeTypeEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static StartTimeTypeEnum valueOf(int value) {
            for (StartTimeTypeEnum flow : StartTimeTypeEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return StartTimeTypeEnum.Not;
        }
    }

    /**
     * 带分
     *
     * @author Administrator
     */
    public enum RatioEnum {
        /**
         * 初始分数
         */
        InitPoint,
        /**
         * 带入分数
         */
        IntoPoint,
        /**
         * 带分系数+初始分
         */
        RatioPoin,

    }

    public enum LeveLEnum {
        /**
         * 顶级
         */
        TopAgentsId(1),
        /**
         * 一级
         */
        OnelevelAgentsId(2),
        /**
         * 会长
         */
        FamilyID(3),
        /**
         * 副会
         */
        Referer(4),
        /**
         * 俱乐部
         */
        Club(5),
        ;
        private int value;

        private LeveLEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

    }

    /**
     * 免费类型
     *
     * @author Administrator
     */
    public enum FreeTypeEnum {
        /**
         * 空
         */
        None,
        /**
         * 总免费次数
         */
        FREE_TOTAL,
        /**
         * 每日免费
         */
        FREE_EVERYDAY,
        /**
         * 每周免费
         */
        FREE_EVERYWEEK,
        ;

        public static FreeTypeEnum valueOf(int value) {
            for (FreeTypeEnum flow : FreeTypeEnum.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return FreeTypeEnum.None;
        }
    }

    /**
     * 消耗类型
     *
     * @author Administrator
     */
    public enum CostTypeEnum {
        /**
         * 空
         */
        None,
        /**
         * 免费
         */
        FREE,
        /**
         * 奖
         */
        PRIZE,
        ;;

        public static CostTypeEnum valueOf(int value) {
            for (CostTypeEnum flow : CostTypeEnum.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return CostTypeEnum.None;
        }
    }


}
