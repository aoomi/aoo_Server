package jsproto.c2s.cclass.club;

/**
 * 亲友圈疲劳值枚举管理
 *
 * @author
 */
public class ClubFatigueEnum {
    /**
     * 疲劳值开关：T:打开，F:关闭
     *
     * @author Administrator
     */
    public enum Club_Fatigue_Open {
        /**
         * 打开
         */
        OPEN(true),
        /**
         * 关闭
         */
        CLOSE(false),
        ;
        private boolean value;

        Club_Fatigue_Open(boolean value) {
            this.value = value;
        }

        public boolean value() {
            return this.value;
        }
    }

    ;


    /**
     * 疲劳值可见：0:管理与成员可见，1:仅管理可见
     *
     * @author Administrator
     */
    public enum Club_Fatigue_Visible {
        MANAGEMENT_AND_MEMBERSHIP_VISIBLE,
        MANAGEMENT_ONLY_VISIBLE,
    }

    ;


    /**
     * 疲劳值显示：0:亲友圈左上角显示当前疲劳值
     *
     * @author Administrator
     */
    public enum Club_Fatigue_Display {
        DISPLAY_CURRENT_FATIGUE_VALUE,
    }

    ;

    /**
     * 疲劳值清零：0:不清零，1:每日清零
     *
     * @author Administrator
     */
    public enum Club_Fatigue_Empty {
        NOT_CLEARED,
        DAILY_CLEARED
    }

    ;

}
