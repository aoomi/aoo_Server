package cenum;

public class FriendsHelpUnfoldRedPackEnum {
    // 任务目标类型
    public enum TargetType {
        None,                        // 0-找不到任务目标类型
        FirstRegisterLoginGame,    // 1-首次注册登陆游戏
        HelpToUnpackRedEnvelopes,    // 2-帮拆红包
        SharingGames,                // 3-分享游戏
        OpenRedPack,                // 4-拆红包
        FriendHuPai,                // 5-好友胡牌
        ;

        public static TargetType valueOf(int value) {
            for (TargetType flow : TargetType.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return TargetType.None;
        }
    }

    // 任务类型
    public enum TaskClassify {
        None, // 找不任务类型
        Novice, // 1-新手任务
        BranchLine, // 2-支线任务
        Infinite, // 3-无限任务
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
        None(0), // 未完成
        Receive(1), // 可领取
        End(2),
        ; // 结束完成
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
     * 红包池类型
     *
     * @author Administrator
     */
    public enum PondType {
        None,                        // 0-空
        NOVICE_POND,                // 1-新手池
        NOT_POND,                    // 2-无门槛15任务
        FRIZEND_HU_PAI_POND,        // 3-好友胡牌池
        ;

        public static PondType valueOf(int value) {
            for (PondType flow : PondType.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return PondType.None;
        }
    }

}
