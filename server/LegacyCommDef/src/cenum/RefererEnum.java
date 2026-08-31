package cenum;


public class RefererEnum {
    /**
     * 推广列表的状态
     *
     * @author Huaxing
     */
    public enum RefererListState {
        //不可
        MustNot(0),
        //允许
        Allow(1),
        //继续游戏
        Complete(2),
        ;
        private int value;

        private RefererListState(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static RefererListState valueOf(int value) {
            for (RefererListState flow : RefererListState.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return RefererListState.MustNot;
        }
    }

}
