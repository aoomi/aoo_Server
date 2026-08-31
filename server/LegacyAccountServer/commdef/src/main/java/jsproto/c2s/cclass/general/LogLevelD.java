package jsproto.c2s.cclass.general;

import lombok.Data;

/**
 * 日志等级
 */
@Data
public class LogLevelD {
    /**
     * 测试
     */
    private int debug;
    /**
     * 错误
     */
    private int error;
    /**
     * 普通
     */
    private int info;
    /**
     * 警告
     */
    private int warn;

}
