package cenum;

/**
 * 玩家行为异常类型记录
 */
public enum ExceptionTypeEnum {
    /**
     * 并发请求异常
     */
    CONCURRENT_EXCEPTION(1, "并发请求异常接口:{%s}"),
    /**
     * 请求接口出现严重超时异常
     */
    INTERFACE_TIMEOUT_EXCEPTION(2, "请求接口出现严重超时异常"),
    /**
     * 分钟内出现超高频率请求操作
     */
    MINUTE_ULTRAHIGH_FREQUENCY_EXCEPTION(3, "分钟内出现超高频率请求操作次数:{%d},接口:{%s}"),

    ;

    /**
     * 参数值
     */
    private int value;
    /**
     * 内容
     */
    private String content;

    ExceptionTypeEnum(int value, String content) {
        this.value = value;
        this.content = content;
    }

    public int getValue() {
        return value;
    }

    public String getContent() {
        return content;
    }
}
