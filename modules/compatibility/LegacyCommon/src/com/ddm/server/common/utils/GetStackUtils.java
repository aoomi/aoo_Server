package com.ddm.server.common.utils;

/**
 * 获取方法调用路径
 */
public class GetStackUtils {
    public static String getGetStackSimple() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < Thread.currentThread().getStackTrace().length; i++) {
            if (i > 0) {
                StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[i];
                if (!stackTraceElement.getClassName().startsWith("java.util.concurrent")
                        && !stackTraceElement.getClassName().startsWith("com.lmax.disruptor")
                        && !stackTraceElement.getClassName().startsWith("com.ddm.server")
                        && !stackTraceElement.getClassName().startsWith("core.network.client2game.handler.PlayerHandler")
                        && !stackTraceElement.getClassName().startsWith("core.network.client2game.handler.BaseHandler")
                        && !stackTraceElement.getClassName().startsWith("java.lang.Thread")
                ) {
                    stringBuilder.append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("->");
                }
            }
        }
        return stringBuilder.toString();
    }

    public static String getGetStack() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < Thread.currentThread().getStackTrace().length; i++) {
            if (i > 0) {
                StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[i];
                stringBuilder.append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("->");
            }
        }
        return stringBuilder.toString();
    }

}
