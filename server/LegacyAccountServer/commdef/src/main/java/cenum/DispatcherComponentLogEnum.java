package cenum;

/**
 * 调度程序组件枚举
 */


public enum DispatcherComponentLogEnum {
    /**
     * 批量日志插入
     */
    BATCH_BD_LOG(0,16384),;
    ;

    private int value;

    private int bufferSize;

    DispatcherComponentLogEnum(int value, int bufferSize) {
        this.value = value;
        this.bufferSize = bufferSize;
    }

    public int id() {
        return this.value;
    }

    public int bufferSize() { return bufferSize; }
}
