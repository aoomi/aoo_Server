package com.ddm.server.common.utils;

public class Enums {
    /**
     * 日志枚举
     *
     * @author Huaxing
     */
    public enum LogEnum {
        //调试
        Debug(0),
        //错误
        Error(1),
        //普通
        Info(2),
        //警告
        Warn(3),
        ;
        private int value;

        private LogEnum(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static LogEnum valueOf(int value) {
            for (LogEnum flow : LogEnum.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return LogEnum.Debug;
        }
    }

}
