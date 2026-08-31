package jsproto.c2s.cclass.task;

/**
 * 活动任务配置枚举
 *
 * @author Administrator
 */
public class TaskConfigEnum {

    // 任务目标类型
    public enum TaskTargetType {
        None,            // 0-找不到任务目标类型
        Invite,            // 1-邀请新用户
        ReferralCode,    // 2-绑定邀请码
        Promoters,        // 3-成为推广员
        Arena,            // 4-进入比赛场完成游戏
        Club,            // 5-加入/创建一个亲友圈
        Certification,    // 6-完成实名认证
        ShareCardType,    // 7-分享X次牌型
        SetCount,        // 8-完成X局游戏
        Share,            // 9-通过微信/朋友圈分享游戏X次
        InviteCount,    // 10-成功邀请新用户统计
        ;

        public static TaskTargetType valueOf(int value) {
            for (TaskTargetType flow : TaskTargetType.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return TaskTargetType.None;
        }
    }

    // 任务类型
    public enum TaskClassify {
        None,        // 找不任务类型
        Novice,    // 1-新手任务
        Daily,        // 2-日常任务
        Special,    // 3-特殊任务
        ;

        public static TaskClassify valueOf(int value) {
            for (TaskClassify flow : TaskClassify.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return TaskClassify.None;
        }


    }

    /**
     * 任务状态
     *
     * @author Administrator
     */
    public enum TaskStateEnum {
        None(0),    // 未完成
        Receive(1),    // 可领取
        End(2),
        ;    // 结束完成
        private int value;

        TaskStateEnum(int value) {
            this.value = value;
        }

        // 获取值
        public int value() {
            return this.value;
        }
    }

    /**
     * 任务时间
     * 1-4(1 每日   2 每周   3 具体时间  4  无)
     *
     * @author Administrator
     */
    public enum TaskTimeEnum {
        // 每日
        Everyday(1),
        // 每周
        Weekly(2),
        // 时间区间
        TimeTnterval(3),
        // 无
        Not(4),
        ;    // 结束完成
        private int value;

        TaskTimeEnum(int value) {
            this.value = value;
        }

        // 获取值
        public int value() {
            return this.value;
        }

        public static TaskTimeEnum valueOf(int value) {
            for (TaskTimeEnum flow : TaskTimeEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return TaskTimeEnum.Not;
        }
    }
}
