package com.ddm.server.common.utils;

public class CommLogD {

	/**
	 * 日志枚举
	 * @author Huaxing
	 *
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
		private LogEnum(int value) {this.value = value;}
		public int value() {return this.value;}
		public static LogEnum valueOf(int value) {
			for (LogEnum flow : LogEnum.values()) {
				if (flow.value == value) {
					return flow;
				}
			}
			return LogEnum.Debug;
		}
	}


	public static void debug(String msg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Debug)) {
			CommLog.debug(msg);
		}
	}

	public static void debug(String msg, Object arg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Debug)) {
			CommLog.debug(msg, arg);
		}
	}

	public static void debug(String msg, Object arg1, Object arg2) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Debug)) {
			CommLog.debug(msg, arg1, arg2);
		}
	}

	public static void debug(String msg, Object... arg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Debug)) {
			CommLog.debug(msg, arg);
		}
	}

	public static void debug(String msg, Throwable t) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Debug)) {
			CommLog.debug(msg, t);
		}
	}

	public static void error(String msg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Error)) {
			CommLog.error(msg);
		}
	}

	public static void error(String msg, Object arg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Error)) {
			CommLog.error(msg, arg);
		}
	}

	public static void error(String msg, Object arg1, Object arg2) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Error)) {
			CommLog.error(msg, arg1, arg2);
		}
	}

	public static void error(String msg, Object... arg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Error)) {
			CommLog.error(msg, arg);
		}
	}

	public static void error(String msg, Throwable t) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Error)) {
			CommLog.error(msg, t);
		}
	}

	public static void info(String msg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Info)) {
			CommLog.info(msg);
		}
	}

	public static void info(String msg, Object arg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Info)) {
			CommLog.info(msg, arg);
		}
	}

	public static void info(String msg, Object arg1, Object arg2) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Info)) {
			CommLog.info(msg, arg1, arg2);
		}
	}

	public static void info(String msg, Object... arg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Info)) {
			CommLog.info(msg, arg);
		}
	}

	public static void info(String msg, Throwable t) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Info)) {
			CommLog.info(msg, t);
		}
	}

	public static void warn(String msg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Warn)) {
			CommLog.warn(msg);
		}
	}

	public static void warn(String msg, Object arg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Warn)) {
			CommLog.warn(msg, arg);
		}
	}

	public static void warn(String msg, Object arg1, Object arg2) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Warn)) {
			CommLog.warn(msg, arg1, arg2);
		}
	}

	public static void warn(String msg, Object... arg) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Warn)) {
			CommLog.warn(msg, arg);
		}
	}

	public static void warn(String msg, Throwable t) {
		if (CommonConfigUtils.isOpenLog(LogEnum.Warn)) {
			CommLog.warn(msg, t);
		}
	}

	public static void initLog() {
		CommLog.initLog();
	}

}
