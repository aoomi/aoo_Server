package cenum;

public class ActivityEnum {

    /**
     * 活动状态
     *
     * @author Administrator
     */
    public enum ActivityStatus {
        None(0),
        /**
         * <code>Open = 1;</code>
         *
         * <pre>
         * 1 - 活动开启 - 能参加且显示
         * </pre>
         */
        Open(1),
        /**
         * <code>Inactive = 2;</code>
         *
         * <pre>
         * 2 - 活动未激活 - 活动的所有事件不触发
         * </pre>
         */
        Inactive(2),
        /**
         * <code>End = 3;</code>
         *
         * <pre>
         * 3 - 活动结束 - 显示但是已经无法参加
         * </pre>
         */
        End(3),
        /**
         * <code>Close = 4;</code>
         *
         * <pre>
         * 4 - 活动关闭 - 不能参加且不显示
         * </pre>
         */
        Close(4),
        ;

        private int value;

        ActivityStatus(int vlaue) {
            this.value = vlaue;
        }

        // 获取值
        public int value() {
            return this.value;
        }

        public static ActivityStatus valueOf(int value) {
            for (ActivityStatus flow : ActivityStatus.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return ActivityStatus.None;
        }

    }


    /**
     * 活动类型
     *
     * @author Administrator
     */
    public enum ActivityType {
        None(0), // 0
        Point(1),//积分排行
        LuckRank(2),
        ;//手气排行
        private int value;

        ActivityType(int value) {
            this.value = value;
        }

        // 获取值
        public int value() {
            return this.value;
        }

        public static ActivityType valueOf(int value) {
            for (ActivityType flow : ActivityType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return ActivityType.None;
        }
    }

    /**
     * 活动时间类型
     *
     * @author Administrator
     */
    public enum ActivityTimeType {
        None(0), // 0
        YTD(1),//年月日
        YTD_HM(2),//年月日——时分
        EveryDay(3),//每日时分
        Weekly(4),//每周时分;

        ;
        private int value;

        ActivityTimeType(int value) {
            this.value = value;
        }

        // 获取值
        public int value() {
            return this.value;
        }

        public static ActivityTimeType valueOf(int value) {
            for (ActivityTimeType flow : ActivityTimeType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return ActivityTimeType.None;
        }

    }


}
