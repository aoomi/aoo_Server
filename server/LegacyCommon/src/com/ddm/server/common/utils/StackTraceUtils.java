package com.ddm.server.common.utils;

import BaseCommon.CommLog;
import java.util.Arrays;

public class StackTraceUtils {

    /**
     * 跟踪日志
     */
    public static void stackTrace(){
        StringBuilder stringBuilder = new StringBuilder();
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                stringBuilder.append("("+stackElements[i].getClassName()+"---->"+stackElements[i].getFileName()+"--->"+stackElements[i].getLineNumber()+"--->"+stackElements[i].getMethodName()+")"+"\n");
            }
        }
        CommLog.error(stringBuilder.toString());
    }

    /**
     * 堆栈输出
     * @param mainError 主错误输出
     * @param e 异常
     */
    public static void stackTrace(String mainError,Exception e){
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(e.getStackTrace()).forEach(m->{
            stringBuilder.append("("+m.getClassName()+"---->"+m.getFileName()+"---->"+m.getMethodName()+"--->"+m.getLineNumber()+")");
        });
        CommLog.error(mainError+":{}        {}",e.getMessage(),stringBuilder.toString());
    }
}
