package cenum;

public class LuckDrawEnum {

    /**
     * 日期类型：0:每日、1:每周、2:具体日期
     */
    public enum LuckDrawDateType {
        /**
         * 每日
         */
        EVERYDAY,
        /**
         * 每周
         */
        WEEKLY,
        /**
         * 具体日期
         */
        EXACT_DATE;

        public static LuckDrawDateType valueOf(int value) {
            for (LuckDrawDateType dateType : LuckDrawDateType.values()) {
                if (dateType.ordinal() == value) {
                    return dateType;
                }
            }
            return EXACT_DATE;
        }
    }

    ;

    /**
     * 时间段 0：全天、1:具体时间
     */
    public enum LuckDrawTimeSlot {
        /**
         * 全天
         */
        ALL_DAY,
        /**
         * 具体时间
         */
        EXACT_TIME,
        ;

        public static LuckDrawTimeSlot valueOf(int value) {
            for (LuckDrawTimeSlot luckDrawTimeSlot : LuckDrawTimeSlot.values()) {
                if (luckDrawTimeSlot.ordinal() == value) {
                    return luckDrawTimeSlot;
                }
            }
            return EXACT_TIME;
        }

    }

    /**
     * 指定人群(0：所有人、1：代理、2：亲友圈、3：联赛)
     */
    public enum LuckDrawAssignCrowd {
        /**
         * 所有人
         */
        ALL,
        /**
         * 代理
         */
        AGENT,
        /**
         * 亲友圈
         */
        CLUB,
        /**
         * 联赛
         */
        UNION,
        ;

        public static LuckDrawAssignCrowd valueOf(int value) {
            for (LuckDrawAssignCrowd crowd : LuckDrawAssignCrowd.values()) {
                if (crowd.ordinal() == value) {
                    return crowd;
                }
            }
            return UNION;
        }
    }


    /**
     * 抽奖类型：0：免费，1：房卡消耗，2：局数，3：大赢家
     */
    public enum LuckDrawType {
        /**
         * 免费
         */
        FREE,
        /**
         * 房卡消耗
         */
        ROOMCARD,
        /**
         * 局数
         */
        SETCOUNT,
        /**
         * 大赢家
         */
        WINNER,
        ;

        public static LuckDrawType valueOf(int value) {
            for (LuckDrawType luckDrawType : LuckDrawType.values()) {
                if (luckDrawType.ordinal() == value) {
                    return luckDrawType;
                }
            }
            return FREE;
        }
    }

    ;

}
