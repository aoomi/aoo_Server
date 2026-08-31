package jsproto.c2s.cclass.rank;

public class RankEnum {
    /**
     * 排行榜查询
     *
     * @author Huaxing
     */
    public enum RankQueryEnum {
        Not(0),
        Set(1),//局数
        Win(2),//赢
        Referer(3),//推广

        ;
        private int value;

        private RankQueryEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static RankQueryEnum valueOf(int value) {
            for (RankQueryEnum flow : RankQueryEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return RankQueryEnum.Not;
        }
    }

}
