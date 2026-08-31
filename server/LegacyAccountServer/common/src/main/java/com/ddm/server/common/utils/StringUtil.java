/*
 * Distributed as part of mchange-commons-java 0.2.6.5
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php
 *
 */
package com.ddm.server.common.utils;

import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class StringUtil {

    final static Pattern COMMA_SEP_TRIM_REGEX;
    final static Pattern COMMA_SEP_NO_TRIM_REGEX;

    static {
        try {
            COMMA_SEP_TRIM_REGEX = Pattern.compile("\\s*\\,\\s*");
            COMMA_SEP_NO_TRIM_REGEX = Pattern.compile("\\,");
        } catch (PatternSyntaxException e) {
            CommLogD.error(null, e);
            throw new InternalError(e.toString());
        }
    }

    public final static String[] EMPTY_STRING_ARRAY = new String[0];

    public static List<Integer> string2Integer(String str) {
        try {
            List<Integer> intList = new ArrayList<>();
            String[] strList = str.split(";");

            for (String item : strList) {
                intList.add(Integer.valueOf(item));
            }
            return intList;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static List<Integer> String2List(String str) {
        if (StringUtils.isNotEmpty(str)) {
            List<Integer> intList = Lists.newArrayList();
            String[] strList = str.split(",");
            for (String item : strList) {
                if (StringUtils.isNumeric(item.trim())) {
                    intList.add(Integer.valueOf(item.trim()));
                }
            }
            return intList;
        } else {
            return Collections.emptyList();
        }
    }

    public static String replaceArgs(String src, String... args) {
        if (args == null || src == null) {
            return src;
        }
        for (int i = 0; i < args.length; i++) {
            src = src.replaceAll("\\{" + i + "\\}", args[i]);
        }
        return src;
    }

    public static String joinList(String[] list, String sep) {
        StringBuffer sb = new StringBuffer();
        if (list == null || list.length == 0) {
            return "";
        }
        for (int i = 0; i < list.length; i++) {
            sb.append(list[i]);
            if (i != list.length - 1) {
                sb.append(sep);
            }
        }

        return sb.toString();
    }

    public static <T> String list2String(List<T> intList) {
        try {
            StringBuilder sBuilder = new StringBuilder();
            for (T num : intList) {
                sBuilder.append(num + ";");
            }
            return sBuilder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static List<String> mailArgs(String str) {
        return stringToList(str, "\t");
    }

    public static List<String> stringToList(String str, String splitor) {
        List<String> ret = new ArrayList<>();
        if (str == null || splitor == null) {
            return ret;
        }
        return Arrays.asList(str.split(splitor));
    }

    public static int compareToBytes(String str, byte[] rawString) {
        byte[] raw1 = str.getBytes(Charset.forName("US-ASCII"));

        int min = Math.min(raw1.length, rawString.length);
        int diff = 0;
        int i;
        for (i = 0; i < min; i++) {
            diff = (raw1[i]) - (rawString[i]);
            if (diff != 0) {
                return diff > 0 ? 1 : -1;
            }
        }
        if (rawString.length == raw1.length) {
            return 0;
        }

        return i == rawString.length ? 1 : -1;
    }

    public static int httpLikeHeader(byte[] input, int size) throws IOException {
        int cur = 0;
        int line_start = 0;
        int state = 0;
        int c = 0;

        int ContentLength = 0;
        boolean firstLine = true;

        while (cur < size) {
            c = input[cur];

            if (state == 0) {
                if (c == '\r') {
                    state = 1;
                }
            } else if (state == 1) {
                if (c == '\n') {
                    state = 2;
                    // process line
                    if (!firstLine) {
                        int lineLen = cur - line_start - 1;
                        if (lineLen > 0) {
                            String line = new String(input, line_start, lineLen);
                            String[] split = line.split(":");
                            if (split.length == 2) {
                                String key = split[0].trim();
                                if ("Content-Length".equals(key)) {
                                    ContentLength = Integer.parseInt(split[1].trim());
                                }
                            }
                        } else {
                            if (ContentLength > 0) {
                                if (cur + ContentLength + 1 <= size) {
                                    return cur + ContentLength + 1;
                                } else {
                                    return -3;
                                }
                            }

                            return cur + 1;
                        }

                    } else {
                        firstLine = false;
                    }
                    state = 0;
                    line_start = cur + 1;
                } else {
                    return -2;
                }
            }
            cur++;
        }
        return -1;
    }


    public static String GetStringBody(String body) throws UnsupportedEncodingException {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        String[] keyV = body.split("&");
        String value = "";
        String key = "";
        for (int i = 0; i < keyV.length; i++) {
            String[] v = keyV[i].split("=");
            for (int j = 0, size = v.length; j < size; j++) {
                key = v[0];
                if (size > 1) {
                    value = v[1];
                } else {
                    value = "";
                }
                value = value.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
                hashMap.put(key, URLDecoder.decode(value, "UTF-8"));
            }
        }
        return GsonUtils.toJsonString(hashMap);
    }


    public static String getMatcher(String regex, String source) {
        String result = "";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            result = matcher.group(1);
        }
        return result;
    }


    /**
     * 判断是否为合法IP * @return the ip
     */
    public static boolean isboolIp(String ipAddress) {
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

    /**
     * 转义正则特殊字符 （$()*+.[]?\^{},|）
     *
     * @param keyword
     * @return
     */
    public static String escapeExprSpecialWord(String keyword) {
        if (!StringUtils.isEmpty(keyword)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < keyword.length(); i++) {
                char src = keyword.charAt(i);
                switch (src) {
                    case '\'':
                        sb.append("''");// hibernate转义多个单引号必须用两个单引号
                        break;
                    case '\"':
                    case '\\':
                        sb.append('\\');
                    default:
                        sb.append(src);
                        break;
                }
            }
            return sb.toString();
        }
        return keyword;
    }

    /**
     * 获取字符长度 中文算两个字符 英文一个
     */
    public static int StringChineseLength(String value) {
        int valueLength = 0;
        String chinese = "[\u4e00-\u9fa5]";
        for (int i = 0; i < value.length(); i++) {
            String temp = value.substring(i, i + 1);
            if (temp.matches(chinese)) {
                valueLength += 2;
            } else {
                valueLength += 1;
            }
        }
        return valueLength;
    }

    /**
     * 是否手机号
     *
     * @param str
     * @return
     */
    public static boolean isPhone(String str) {
        if (str == null || str.length() <= 0) {
            return false;
        }
        // TODO 2020/07/10 傅哥说：不用再验证手机号是否正确
//        String phtonStr = "^1[3456789]\\d{9}$";
//        Pattern pattern = Pattern.compile(phtonStr);
//        Matcher isNum = pattern.matcher(str);
//        if (!isNum.matches()) {
//            return false;
//        }
        return true;
    }

    /**
     * 获取处理后的手机号
     *
     * @param str
     * @return
     */
    public static String getPhone(String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        if (str.length() <= 11) {
            return str.replaceAll("(\\d{3})\\d{4}(\\d)", "$1****$2");
        } else {
            return str.replaceAll("(\\d{5})\\d{6}(\\d)", "$1****$2");
        }
    }


    public static final String REGEX_MOBILE = "(13[0-9]|14[579]|15[0-3,5-9]|16[6]|17[0135678]|18[0-9]|19[89])\\d{8}";

    /**
     * 匹配文本中的电话号码，并将中间四位替换为*
     * @param content 内容
     * @return
     */
    public static String regexMobile(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        Pattern p = Pattern.compile(REGEX_MOBILE);
        Matcher m = p.matcher(content);
        String paramStr = new String(content);
        while (m.find()) {
            //一定需要先查找再调用group获取电话号码
            String group = m.group();
            paramStr = paramStr.replaceAll(group, group.substring(0, 3) + "****" + group.substring(7, 11));
        }
        return paramStr;
    }


    public static boolean isNumber(String str){
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        String reg = "^[0-9]+(.[0-9]+)?$";
        return str.matches(reg);
    }

    /**
     * 邮箱规则检查
     * @param email
     * @return
     */
    public static boolean checkEmail(String email) {
        boolean flag = false;
        try {
            String check = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
            Pattern regex = Pattern.compile(check);
            Matcher matcher = regex.matcher(email);
            flag = matcher.matches();
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }
}
