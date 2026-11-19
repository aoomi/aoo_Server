package com.ddm.server.common.utils;

import com.ddm.server.common.config.CommonConfigInfo;

import java.util.Set;

public class CommonConfigUtils {
    private static CommonConfigInfo commonConfigInfo;
    private static Set<CommLogD.LogEnum> LogLevelSet;

    public static void setCommonConfig(CommonConfigInfo commonConfigInfo) {
        CommonConfigUtils.commonConfigInfo  = commonConfigInfo;
    }
    /**
     * 是否开启指定日志
     * @param logEnum
     * @return
     */
    public final static boolean isOpenLog(CommLogD.LogEnum logEnum) {
        return commonConfigInfo.getLogLevelSet().contains(logEnum);
    }

}
