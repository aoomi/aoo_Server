/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.common.utils;

import com.ddm.server.common.CommLogD;
import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;


/**
 *
 */
public class NetUtil {

    public static String getHostName() {
        try {
            InetAddress ia = InetAddress.getLocalHost();
            return ia.getHostName();
        } catch (UnknownHostException ex) {
            CommLogD.error(NetUtil.class.getName(), ex);
        }
        return null;
    }

    /**
     * 获取整数形式的主机IP地址
     *
     * @return
     */
    public static int getHostIpInteger() {
        InetAddress ip = getHostIpAddr();
        if (ip == null) {
            return 0;
        }

        return ipAddressToInt(ip);
    }

    /**
     * IP地址转为整数
     *
     * @param addr
     * @return
     */
    public static int ipAddressToInt(InetAddress addr) {
        byte[] ip_arr = addr.getAddress();
        // big endian
        int res = ((ip_arr[3] & 0xff)) | ((ip_arr[2] & 0xff) << 8) | ((ip_arr[1] & 0xff) << 16) | ((ip_arr[0] & 0xff) << 24);
        return res;
    }

    /**
     * 通过IP地址接口获取第一个可用的IP地址
     *
     * @return
     */
    public static InetAddress getHostIpAddr() {
        Enumeration<NetworkInterface> niEnums;
        try {
            niEnums = NetworkInterface.getNetworkInterfaces();
            while (niEnums.hasMoreElements()) {
                NetworkInterface ni = niEnums.nextElement();
                if (ni.isLoopback() || ni.isPointToPoint() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = ni.getInetAddresses();

                while (addrs.hasMoreElements()) {
                    InetAddress ip = addrs.nextElement();
                    if (ip != null && (ip instanceof Inet4Address) && !ip.isAnyLocalAddress() && !ip.isMulticastAddress()) {
                        return ip;
                    }
                }

            }

        } catch (SocketException ex) {
            CommLogD.error(NetUtil.class.getName(), ex);
        }

        return null;
    }

    /**
     * 将xxx.xxx.xxx.xxx形式的ip字符串转换为字节数组
     *
     * @param ipString
     * @return
     */
    public static byte[] ipAddressStringToRaw(String ipString) {
        String[] ipSegs = ipString.split("\\.");
        if (ipSegs.length != 4) {
            return null;
        }
        byte[] targets = new byte[4];
        targets[0] = (byte) (Integer.parseInt(ipSegs[0]) & 0xff);
        targets[1] = (byte) (Integer.parseInt(ipSegs[1]) & 0xff);
        targets[2] = (byte) (Integer.parseInt(ipSegs[2]) & 0xff);
        targets[3] = (byte) (Integer.parseInt(ipSegs[3]) & 0xff);

        return targets;
    }

    /**
     * 将xxx.xxx.xxx.xxx形式的ip字符串转换为INT整数
     *
     * @param ipString
     * @return
     */
    public static int ipAddressStringToInt(String ipString) {
        byte[] targets = ipAddressStringToRaw(ipString);
        if (targets == null) {
            return 0;
        }
        // big endian
        int res = ((targets[3] & 0xff)) | ((targets[2] & 0xff) << 8) | ((targets[1] & 0xff) << 16) | ((targets[0] & 0xff) << 24);
        return res;
    }

    /**
     * 将xxx.xxx.xxx.xxx形式的ip字符串转换为IP地址
     *
     * @param ipString
     * @return
     * @throws UnknownHostException
     */
    public static InetAddress ipAddressStringToInetAddr(String ipString) throws UnknownHostException {
        byte[] targets = ipAddressStringToRaw(ipString);
        if (targets == null) {
            throw new UnknownHostException(ipString);
        }

        return InetAddress.getByAddress(targets);
    }

    /**
     * 将INT表示的IP地址转换为xxx.xxx.xxx.xxx形式
     *
     * @param ip
     * @return
     */
    public static String intToIpAddressString(int ip) {
        int[] targets = new int[4];
        targets[3] = (ip & 0xFF);
        targets[2] = (ip >> 8 & 0xFF);
        targets[1] = (ip >> 16 & 0xFF);
        targets[0] = (ip >> 24 & 0xFF);

        return String.format("%d.%d.%d.%d", (int) targets[0], (int) targets[1], (int) targets[2], (int) targets[3]);
    }

    public static final String _255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    public static final Pattern pattern = Pattern.compile("^(?:" + _255 + "\\.){3}" + _255 + "$");

    public static String longToIpV4(long longIp) {
        int octet3 = (int) ((longIp >> 24) % 256);
        int octet2 = (int) ((longIp >> 16) % 256);
        int octet1 = (int) ((longIp >> 8) % 256);
        int octet0 = (int) ((longIp) % 256);
        return octet3 + "." + octet2 + "." + octet1 + "." + octet0;
    }

    public static long ipV4ToLong(String ip) {
        String[] octets = ip.split("\\.");
        return (Long.parseLong(octets[0]) << 24) + (Integer.parseInt(octets[1]) << 16)
                + (Integer.parseInt(octets[2]) << 8) + Integer.parseInt(octets[3]);
    }

    public static final String LOCALHOST = "127.0.0.1";


    public static boolean isIPv4Private(String ip) {
        if (LOCALHOST.equals(ip)) {
            return true;
        }
        long longIp = ipV4ToLong(ip);
        return (longIp >= ipV4ToLong("10.0.0.0") && longIp <= ipV4ToLong("10.255.255.255"))
                || (longIp >= ipV4ToLong("172.16.0.0") && longIp <= ipV4ToLong("172.31.255.255"))
                || longIp >= ipV4ToLong("192.168.0.0") && longIp <= ipV4ToLong("192.168.255.255");
    }

    public static boolean isIPv4Valid(String ip) {
        return pattern.matcher(ip).matches();
    }

}
