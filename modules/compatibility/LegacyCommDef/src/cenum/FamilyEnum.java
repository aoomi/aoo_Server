package cenum;


public class FamilyEnum {
    /**
     * 绑定工会
     *
     * @author Huaxing
     */
    public enum BindingFamilyEnum {
        Not(0),
        //查看
        Select(1),
        //检查认证
        Check(2),
        //绑定工会
        BinDing(3),
        ;
        private int value;

        BindingFamilyEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static BindingFamilyEnum valueOf(int value) {
            for (BindingFamilyEnum flow : BindingFamilyEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return BindingFamilyEnum.Select;
        }
    }

}
