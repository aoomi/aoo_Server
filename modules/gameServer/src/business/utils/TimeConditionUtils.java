package business.utils;

import com.ddm.server.common.utils.CommTime;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import jsproto.c2s.cclass.club.Club_define;

/**
 * 时间条件
 *
 * @author 用于公共时间赛选, 如赛选 当天、昨天、近三天、近三十天数据
 */
public class TimeConditionUtils {

    /**
     * 获取时间 0今天,1昨天,2最近三天
     */
    public enum Record_Get_Type {
        /**
         * 今天
         */
        RECORD_GET_TYPE_TODAY(0),
        /**
         * 昨天
         */
        RECORD_GET_TYPE_YESTERDAY(1),
        /**
         * 最近三天
         */
        RECORD_GET_TYPE_LAST_THREE_DAYS(2),
        /**
         * 近一个月
         */
        RECORD_GET_TYPE_MONTH(3),

        /**
         * 近一个月
         */
        RECORD_GET_DAY_15(4),
        /**
         * 近七天
         */
        RECORD_GET_DAY_7(5),
        /**
         * 第三天
         */
        RECORD_GET_DAY_3(6),
        ;

        private int value;

        Record_Get_Type(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Record_Get_Type getRecordType(String value) {
            String gameTypyName = value.toUpperCase();
            for (Record_Get_Type flow : Record_Get_Type.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Record_Get_Type.RECORD_GET_TYPE_TODAY;
        }

        public static Record_Get_Type valueOf(int value) {
            for (Record_Get_Type flow : Record_Get_Type.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Record_Get_Type.RECORD_GET_TYPE_TODAY;
        }
    }

    ;


    /**
     * 时间条件
     * 小于指定时间
     *
     * @param type 类型
     * @return
     */
    public static Criteria DayZeroClockSLT(int type) {
        return DayZeroClockSLT("dateTime", type);
    }

    public static Criteria DayZeroClockSLT(String propertyName, int type) {
        switch (Record_Get_Type.valueOf(type)) {
            case RECORD_GET_TYPE_LAST_THREE_DAYS:
                // 近三天，今天、昨天、前天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(3));
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(1));
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(2));
            case RECORD_GET_TYPE_MONTH:
                // 近30天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(31));
            case RECORD_GET_DAY_15:
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(15));
            case RECORD_GET_DAY_7:
                // 近7天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(7));
            default:
                return null;
        }
    }

    /**
     * 中至记录
     *
     * @param propertyName
     * @param type
     * @return
     */
    public static Criteria DayZeroClockSixZhongZhi(String propertyName, int type) {
        switch (Club_define.CLUB_PROMOTION_TIME_TYPE.valueOf(type)) {
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return Restrictions.between(propertyName, CommTime.getCycleNowTime6YMD(), CommTime.nowSecond());
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(1), CommTime.getCycleNowTime6YMD());
            case RECORD_GET_TYPE_TWO:
                // 昨天
                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(2), CommTime.getYesterDay6ByCount(1));
            case RECORD_GET_TYPE_THREE:
                // 昨天
                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(3), CommTime.getYesterDay6ByCount(2));
            case RECORD_GET_TYPE_FOUR:
                // 昨天

                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(4), CommTime.getYesterDay6ByCount(3));
            case RECORD_GET_TYPE_FIVE:
                // 昨天
                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(5), CommTime.getYesterDay6ByCount(4));
            case RECORD_GET_TYPE_SIX:
                // 昨天
                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(6), CommTime.getYesterDay6ByCount(5));
            default:
                return null;
        }
    }

    /**
     * 时间条件
     *
     * @param type 类型
     * @return
     */
    public static Criteria DayZeroClockS(int type) {
        switch (Record_Get_Type.valueOf(type)) {
            case RECORD_GET_TYPE_LAST_THREE_DAYS:
                // 近三天，今天、昨天、前天
                return Restrictions.ge("dateTime", CommTime.getYesterDayStringYMD(2));
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return Restrictions.eq("dateTime", CommTime.getNowTimeStringYMD());
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(1));
            case RECORD_GET_TYPE_MONTH:
                // 近30天
                return Restrictions.ge("dateTime", CommTime.getYesterDayStringYMD(30));
            case RECORD_GET_DAY_7:
                // 近30天
                return Restrictions.ge("dateTime", CommTime.getYesterDayStringYMD(7));
            case RECORD_GET_DAY_3:
                // 前天
                return Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(2));
            default:
                return null;
        }
    }

    /**
     * 中至排行榜时间条件
     *
     * @param type 类型
     * @return
     */
    public static Criteria DayZeroClockSZhongZhiRanked(int type) {
        switch (Record_Get_Type_ZhongZhi.valueOf(type)) {
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return Restrictions.eq("dateTime", CommTime.getNowTimeStringYMD());
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(1));
            case RECORD_GET_TYPE_THIS_WEEK:
                // 本周
                return Restrictions.between("dateTime", CommTime.getSecToYMDStr(CommTime.getFirstDayOfWeekZeroClockS()), CommTime.getNowTimeStringYMD());
            case RECORD_GET_TYPE_lAST_WEEK:
                // 上周
                return Restrictions.between("dateTime", CommTime.getSecToYMDStr(CommTime.getFirstDayOfLatsWeekZeroClockS()), CommTime.getSecToYMDStr(CommTime.getFirstDayOfWeekZeroClockS()));

            default:
                return null;
        }
    }

    /**
     * 对应的字段 和事件类别
     *
     * @param type 类型
     * @return
     */
    public static Criteria CLUBDayZeroClockS(String propertyName, int type) {
        switch (Club_define.CLUB_PROMOTION_TIME_TYPE.valueOf(type)) {
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return Restrictions.eq(propertyName, CommTime.getNowTimeStringYMD());
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(1));
            case RECORD_GET_TYPE_TWO:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(2));
            case RECORD_GET_TYPE_THREE:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(3));
            case RECORD_GET_TYPE_FOUR:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(4));
            case RECORD_GET_TYPE_FIVE:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(5));
            case RECORD_GET_TYPE_SIX:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDayStringYMD(6));
            default:
                return null;
        }
    }

    /**
     * 对应的字段 和事件类别
     *
     * @param type 类型
     * @return
     */
    public static Criteria CLUBDayZeroClockSZhongZhi(String propertyName, int type) {
        switch (Club_define.CLUB_PROMOTION_TIME_TYPE.valueOf(type)) {
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return Restrictions.eq(propertyName, String.valueOf(CommTime.getNowTime6YMD()));
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDay6ByCount(1));
            case RECORD_GET_TYPE_TWO:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDay6ByCount(2));
            case RECORD_GET_TYPE_THREE:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDay6ByCount(3));
            case RECORD_GET_TYPE_FOUR:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDay6ByCount(4));
            case RECORD_GET_TYPE_FIVE:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDay6ByCount(5));
            case RECORD_GET_TYPE_SIX:
                // 昨天
                return Restrictions.eq(propertyName, CommTime.getYesterDay6ByCount(6));
            default:
                return null;
        }
    }

    /**
     * 对应的字段 和事件类别
     *
     * @param type 类型
     * @return
     */
    public static Criteria CLUBDayZeroClockSZhongZhiBetween(String propertyName, int type) {
        switch (Club_define.CLUB_PROMOTION_TIME_TYPE.valueOf(type)) {
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return Restrictions.between(propertyName, String.valueOf(CommTime.getNowTime6YMD()), CommTime.nowSecond());
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(1), String.valueOf(CommTime.getNowTime6YMD()));
            case RECORD_GET_TYPE_TWO:
                // 昨天

                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(2), CommTime.getYesterDay6ByCount(1));
            case RECORD_GET_TYPE_THREE:
                // 昨天

                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(3), CommTime.getYesterDay6ByCount(2));
            case RECORD_GET_TYPE_FOUR:
                // 昨天

                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(4), CommTime.getYesterDay6ByCount(3));
            case RECORD_GET_TYPE_FIVE:
                // 昨天

                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(5), CommTime.getYesterDay6ByCount(4));
            case RECORD_GET_TYPE_SIX:
                // 昨天
                return Restrictions.between(propertyName, CommTime.getYesterDay6ByCount(6), CommTime.getYesterDay6ByCount(5));
            default:
                return null;
        }
    }

    /**
     * 时间条件
     * 联合查询使用
     *
     * @param type 类型
     * @return
     */
    public static Criteria DayZeroClockSLeftJoin(int type) {
        switch (Record_Get_Type.valueOf(type)) {
            case RECORD_GET_TYPE_LAST_THREE_DAYS:
                // 近三天，今天、昨天、前天
                return Restrictions.geJoin("dateTime", CommTime.getYesterDayStringYMD(2), "r");
            case RECORD_GET_TYPE_TODAY:
                // 今天
                return Restrictions.eqJoin("dateTime", CommTime.getNowTimeStringYMD(), "r");
            case RECORD_GET_TYPE_YESTERDAY:
                // 昨天
                return Restrictions.eqJoin("dateTime", CommTime.getYesterDayStringYMD(1), "r");
            case RECORD_GET_TYPE_MONTH:
                // 近30天
                return Restrictions.geJoin("dateTime", CommTime.getYesterDayStringYMD(30), "r");
            default:
                return null;
        }
    }

    /**
     * 获取时间 0今天,1昨天,2本周 3上周
     */
    public enum Record_Get_Type_ZhongZhi {
        /**
         * 今天
         */
        RECORD_GET_TYPE_TODAY(0),
        /**
         * 昨天
         */
        RECORD_GET_TYPE_YESTERDAY(1),
        /**
         * 这周
         */
        RECORD_GET_TYPE_THIS_WEEK(2),
        /**
         * 上周
         */
        RECORD_GET_TYPE_lAST_WEEK(3),

        ;

        private int value;

        Record_Get_Type_ZhongZhi(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Record_Get_Type_ZhongZhi getRecordType(String value) {
            String gameTypyName = value.toUpperCase();
            for (Record_Get_Type_ZhongZhi flow : Record_Get_Type_ZhongZhi.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Record_Get_Type_ZhongZhi.RECORD_GET_TYPE_TODAY;
        }

        public static Record_Get_Type_ZhongZhi valueOf(int value) {
            for (Record_Get_Type_ZhongZhi flow : Record_Get_Type_ZhongZhi.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Record_Get_Type_ZhongZhi.RECORD_GET_TYPE_TODAY;
        }
    }

    ;

}
