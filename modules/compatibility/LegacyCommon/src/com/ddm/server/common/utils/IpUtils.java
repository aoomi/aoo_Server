package com.ddm.server.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class IpUtils {


    /**
     * 获取Ip地址
     *
     * @param request
     * @return
     */
    public static String getIpAddress(String remoteAddress, Function<String, String> header) {
        Set<String> trustedProxies = new HashSet<>(Arrays.asList(
                System.getProperty("TrustedProxyIP", "").split(",")));
        // Forwarding headers are attacker-controlled unless the direct peer is a
        // reverse proxy explicitly trusted by deployment configuration.
        if (!trustedProxies.contains(remoteAddress)) {
            return remoteAddress;
        }
        String Xip = header.apply("X-Real-IP");
        String XFor = header.apply("X-Forwarded-For");
        if (StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = XFor.indexOf(",");
            if (index != -1) {
                return XFor.substring(0, index);
            } else {
                return XFor;
            }
        }
        XFor = Xip;
        if (StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)) {
            return XFor;
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = header.apply("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = header.apply("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = header.apply("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = header.apply("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = remoteAddress;
        }
        return XFor;
    }

}
