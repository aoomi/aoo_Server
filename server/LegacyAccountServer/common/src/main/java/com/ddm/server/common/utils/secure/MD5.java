package com.ddm.server.common.utils.secure;



import com.ddm.server.common.utils.CommLogD;

import java.security.MessageDigest;


public class MD5 {

    /***
     * MD5加码 生成32位md5码
     */
    public static String md5(String inStr) {
        MessageDigest md5 = null;
        byte[] byteArray = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byteArray = inStr.getBytes("utf-8");
        } catch (Exception e) {
            CommLogD.error("生成MD5串错误");
            return "";
        }
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}
