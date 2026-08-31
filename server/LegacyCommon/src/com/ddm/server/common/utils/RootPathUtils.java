package com.ddm.server.common.utils;

import java.io.File;

/**
 * @author xsj
 * @date 2020/8/25 16:25
 * @description 获取工程路径
 */
public class RootPathUtils {
    public static String getPath(Class<?> clazz) {
        File f = new File(clazz.getResource("/").getPath());
        String path = f.getPath();
//        System.out.println(path);
        return path;
    }
}
