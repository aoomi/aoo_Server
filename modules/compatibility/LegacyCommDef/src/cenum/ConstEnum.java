package cenum;

public class ConstEnum {
    // 邮件类型
    public enum MailType {
        None, // 0-无
        Message, // 1-消息邮件
        Prize, // 2-奖励邮件
        Custom, // 3-自定义邮件
        CustomReward, // 4-自定义奖励邮件
    }


    // 资源(货币、物品等)操作类型
    public enum ResOpType {
        None, // 0-
        Gain, // 1-获得
        Lose, // 2-消耗
        Fallback,
        ; // 3-回退

        public static ResOpType valueOf(int value) {
            for (ResOpType flow : ResOpType.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return ResOpType.None;
        }
    }

    /**
     * 充值类型
     *
     * @author Huaxing
     */
    public enum RechargeType {
        Not(0), DirectCharge(1), // 直充
        Platform(2), // 平台
        Exchange(3), // 兑换
        Reward(4), // 奖励
        Login_In(6), // 连续登录
        LuckDraw(7), // 抽奖
        CLUB(8), HuRewardShare(9), // 胡牌奖励分享
        TaskReward(10),// 任务奖励
        Family(11),//代理
        Phone(12),//手机
        ;
        private int value;

        private RechargeType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static RechargeType valueOf(int value) {
            for (RechargeType flow : RechargeType.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return RechargeType.DirectCharge;
        }
    }
}
