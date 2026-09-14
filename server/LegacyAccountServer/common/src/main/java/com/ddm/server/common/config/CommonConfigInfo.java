package com.ddm.server.common.config;

import com.ddm.server.common.utils.CommLogD;
import lombok.Data;

import java.util.Set;

@Data
public class CommonConfigInfo {
    /**
     * 日志等级
     */
    private Set<CommLogD.LogEnum> LogLevelSet;
}
