package cenum.room;

public class RoomContinueEnum {

    public enum RoomContinueTypeEnum {
        // 不继续
        NOT_Continue(-1),
        // 我来续局
        Myself_Continue(0),
        // 平分续局
        Equally_Continue(1),
        // 大赢家续局
        BigWinner_Continue(2),
        // 亲友圈 大联盟续局
        Club_Continue(3),
        ;
        private int value;

        private RoomContinueTypeEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static RoomContinueTypeEnum valueOf(int value) {
            for (RoomContinueTypeEnum flow : RoomContinueTypeEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return RoomContinueTypeEnum.NOT_Continue;
        }

    }

    public enum RoomContinueTimeEnum {
        // 五分钟
        FiveMinute(300),
        ;
        private int value;

        private RoomContinueTimeEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static RoomContinueTimeEnum valueOf(int value) {
            for (RoomContinueTimeEnum flow : RoomContinueTimeEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return RoomContinueTimeEnum.FiveMinute;
        }

    }


}
