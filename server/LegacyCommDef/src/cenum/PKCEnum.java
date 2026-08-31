package cenum;

public class PKCEnum {
    public static enum SSSDiFen {
        Y(1),
        R(2),
        W(5);
        private int value;

        private SSSDiFen(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static SSSDiFen valueOf(int value) {
            for (SSSDiFen flow : SSSDiFen.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return SSSDiFen.Y;
        }
    }

    public static enum SSSDaQiang {
        Y(1),
        R(2),
        S(3);
        private int value;

        private SSSDaQiang(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static SSSDaQiang valueOf(int value) {
            for (SSSDaQiang flow : SSSDaQiang.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return SSSDaQiang.Y;
        }
    }

}
