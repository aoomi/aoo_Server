package cenum.room;

/**
 * 付费方式
 *
 * @author Administrator
 */
public enum PaymentRoomCardType {

    PaymentRoomCardType_HomeOwerPay(0), // 房主付
    PaymentRoomCardType_AutoPay(1), // 平分支付
    PaymentRoomCardType_WinnerPay(2), // 大赢家付
    ;

    private int value;

    private PaymentRoomCardType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static PaymentRoomCardType valueOf(int value) {
        for (PaymentRoomCardType flow : PaymentRoomCardType.values()) {
            if (flow.value == value) {
                return flow;
            }
        }
        return PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay;
    }
}