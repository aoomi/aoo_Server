package cenum.pk;

/**
 * 麻将操作错误编码
 */
public enum PKOpCardError {
    /**
     * 成功
     */
    SUCCESS(0),
    /**
     * 错误动作类型
     */
    ERROR_OP_TYPE(-1),
    /**
     * 动作排队中
     */
    LINE_UP(-2),
    /**
     * 错误执行动作
     */
    ERROR_EXEC_OP_TYPE(-3),

    /**
     * 检查操作错误
     */
    CHECK_OP_TYPE_ERROR(-4),

    /**
     * 空操作
     */
    NONE(-5),

    /**
     * 重复执行
     */
    REPEAT_EXECUTE(-6),

    /**
     * 回合操作位置错误
     */
    ROUND_POS_ERROR(-7),

    /**
     * 机器人操作错误
     */
    ROBOT_OP_ERROR(-8),
    ;
    int value;

    PKOpCardError(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static void main(String[] ars) {
        System.out.println(PKOpCardError.NONE.value());
    }
}
