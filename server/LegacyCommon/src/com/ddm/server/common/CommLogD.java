package com.ddm.server.common;

import BaseCommon.CommLog;
import com.ddm.server.common.utils.Enums.LogEnum;

public class CommLogD {

    public static void debug(String msg) {
        if (Config.isOpenLog(LogEnum.Debug)) {
            CommLog.debug(msg);
        }
    }

    public static void debug(String msg, Object arg) {
        if (Config.isOpenLog(LogEnum.Debug)) {
            CommLog.debug(msg, arg);
        }
    }

    public static void debug(String msg, Object arg1, Object arg2) {
        if (Config.isOpenLog(LogEnum.Debug)) {
            CommLog.debug(msg, arg1, arg2);
        }
    }

    public static void debug(String msg, Object... arg) {
        if (Config.isOpenLog(LogEnum.Debug)) {
            CommLog.debug(msg, arg);
        }
    }

    public static void debug(String msg, Throwable t) {
        if (Config.isOpenLog(LogEnum.Debug)) {
            CommLog.debug(msg, t);
        }
    }

    public static void error(String msg) {
        if (Config.isOpenLog(LogEnum.Error)) {
            CommLog.error(msg);
        }
    }

    public static void error(String msg, Object arg) {
        if (Config.isOpenLog(LogEnum.Error)) {
            CommLog.error(msg, arg);
        }
    }

    public static void error(String msg, Object arg1, Object arg2) {
        if (Config.isOpenLog(LogEnum.Error)) {
            CommLog.error(msg, arg1, arg2);
        }
    }

    public static void error(String msg, Object... arg) {
        if (Config.isOpenLog(LogEnum.Error)) {
            CommLog.error(msg, arg);
        }
    }

    public static void error(String msg, Throwable t) {
        if (Config.isOpenLog(LogEnum.Error)) {
            CommLog.error(msg, t);
        }
    }

    public static void info(String msg) {
        if (Config.isOpenLog(LogEnum.Info)) {
            CommLog.info(msg);
        }
    }

    public static void info(String msg, Object arg) {
        if (Config.isOpenLog(LogEnum.Info)) {
            CommLog.info(msg, arg);
        }
    }

    public static void info(String msg, Object arg1, Object arg2) {
        if (Config.isOpenLog(LogEnum.Info)) {
            CommLog.info(msg, arg1, arg2);
        }
    }

    public static void info(String msg, Object... arg) {
        if (Config.isOpenLog(LogEnum.Info)) {
            CommLog.info(msg, arg);
        }
    }

    public static void info(String msg, Throwable t) {
        if (Config.isOpenLog(LogEnum.Info)) {
            CommLog.info(msg, t);
        }
    }

    public static void warn(String msg) {
        if (Config.isOpenLog(LogEnum.Warn)) {
            CommLog.warn(msg);
        }
    }

    public static void warn(String msg, Object arg) {
        if (Config.isOpenLog(LogEnum.Warn)) {
            CommLog.warn(msg, arg);
        }
    }

    public static void warn(String msg, Object arg1, Object arg2) {
        if (Config.isOpenLog(LogEnum.Warn)) {
            CommLog.warn(msg, arg1, arg2);
        }
    }

    public static void warn(String msg, Object... arg) {
        if (Config.isOpenLog(LogEnum.Warn)) {
            CommLog.warn(msg, arg);
        }
    }

    public static void warn(String msg, Throwable t) {
        if (Config.isOpenLog(LogEnum.Warn)) {
            CommLog.warn(msg, t);
        }
    }

    public static void initLog() {
        CommLog.initLog();
    }

}
