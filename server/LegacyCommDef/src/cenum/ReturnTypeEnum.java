package cenum;

//private int returnType = 0;//1：扣除直充,2:扣除平台,3:直充、平台都扣除,4,5,41,6,32
public enum ReturnTypeEnum {

    UNKWON(0), // 未知类型
    DIRECTCHANGE(1), // 直充
    PLATFORM(2), // 平台
    RECHANGEANDPLATFORM(3),//直充 和 平台
//	CLUB(4),//俱乐部
//	CLUBANDCHANGE(5),//俱乐部 和 直充
//	CLUBANDPLATFORM(6),//俱乐部 和 平台
//	ALL(7),//俱乐部  直充 平台
    ;

    private int value;

    private ReturnTypeEnum(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static ReturnTypeEnum valueOf(int value) {
        for (ReturnTypeEnum flow : ReturnTypeEnum.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return ReturnTypeEnum.UNKWON;
    }
}
