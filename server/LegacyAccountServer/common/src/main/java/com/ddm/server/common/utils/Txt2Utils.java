package com.ddm.server.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

public class Txt2Utils {
    /**
     * 读取txt文件的内容
     *
     * @param file 想要读取的文件对象
     * @return 返回文件内容
     */
    public static String txt2String(File file, String encoding) {
        StringBuilder result = new StringBuilder();
        try {
            InputStreamReader read = null;// 考虑到编码格式
            BufferedReader br = null;//构造一个BufferedReader类来读取文件
            try {
                read = new InputStreamReader(new FileInputStream(file), encoding);// 考虑到编码格式
                br = new BufferedReader(read);//构造一个BufferedReader类来读取文件
                String s = null;
                while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                    result.append(System.lineSeparator() + s);
                }
            } catch (Exception e) {
                CommLogD.error("txtFile to String error fileName:" + file.getName());
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            } finally {
                if (br != null) {
                    br.close();
                }
                if (read != null) {
                    read.close();
                }
            }
        } catch (Exception e) {
            CommLogD.error("txt2String：" + e.getMessage());
        }
        return result.toString();
    }

    /**
     * 读取txt文件的内容
     *
     * @param file 想要读取的文件对象
     * @return 返回文件内容
     */
    public static String txt2String(File file) {
        StringBuilder result = new StringBuilder();
        try {
            InputStreamReader read = null;// 考虑到编码格式
            BufferedReader br = null;//构造一个BufferedReader类来读取文件
            try {
                String encoding = "GBK";
                read = new InputStreamReader(new FileInputStream(file), encoding);// 考虑到编码格式
                br = new BufferedReader(read);//构造一个BufferedReader类来读取文件
                String s = null;
                while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                    result.append(System.lineSeparator() + s);
                }
            } catch (Exception e) {
                CommLogD.error("txtFile to String error fileName:" + file.getName());
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            } finally {
                if (br != null) {
                    br.close();
                }
                if (read != null) {
                    read.close();
                }
            }
        } catch (Exception e) {
            CommLogD.error("txt2String：" + e.getMessage());
        }
        return result.toString();
    }

    /**
     * 文本转换为map
     *
     * @param filPath  文件路径
     * @param fileName 文件名
     * @param encoding 编码
     * @return
     */
    public static Map<String, String> txt2Map(String filPath, String fileName, String encoding) {
        Map<String, String> result = new HashMap<String, String>();
        try {
            File file = new File(filPath + fileName);
            if (Objects.isNull(file)) {
                return result;
            }
            if(!file.exists()) {
                return Maps.newMap();
            }
            // 考虑到编码格式
            InputStreamReader read = null;
            // 构造一个BufferedReader类来读取文件
            BufferedReader br = null;
            try {
                // 考虑到编码格式
                read = new InputStreamReader(new FileInputStream(file), encoding);
                // 构造一个BufferedReader类来读取文件
                br = new BufferedReader(read);
                String s = null;
                // 使用readLine方法，一次读一行
                while (Objects.nonNull((s = br.readLine()))) {
                    if (StringUtils.isEmpty(s)) {
                        continue;
                    }
                    String regex = "^(#|\\*|/*|//*/).(([\u0391-\uFFE5]+)|\\w+|.)";  //比配注释
                    if (s.matches(regex)) {
                        continue;
                    }
                    if (s.indexOf("=") == -1) {
                        continue;
                    }
                    int find = s.indexOf("=");
                    int lastPos = s.substring(find + 1).lastIndexOf(";");
                    if (lastPos != -1) {
                        result.put(s.substring(0, find).trim(), s.substring(find + 1, find + 1 + lastPos).trim());
                    } else {
                        result.put(s.substring(0, find).trim(), s.substring(find + 1).trim());
                    }
                }
            } catch (Exception e) {
                CommLogD.error("txtFile to Map error file:" + filPath + fileName);
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            } finally {
                if (Objects.nonNull(br)) {
                    br.close();
                }
                if (Objects.nonNull(read)) {
                    read.close();
                }
            }
        } catch (Exception e) {
            CommLogD.error("txt2Map：" + e.getMessage());
        }
        return result;
    }

    /*
     * 字符串转换为数组 integer
     * */
    public static ArrayList<Integer> String2ListInteger(String str) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if (StringUtils.isEmpty(str)) {
            return result;
        }
        String txt = str.substring(str.indexOf("[") + 1, str.indexOf("]"));
        String[] strArray = txt.split(",");
        for (int i = 0; i < strArray.length; i++) {
            String temp = strArray[i];
            temp = temp.trim();
            if ("".equals(temp)) {
                continue;
            }
            if (temp.startsWith("0x") || temp.startsWith("0X")) {
                result.add(Integer.parseInt(temp.substring(2), 16));
            } else {
                result.add(Integer.parseInt(temp));
            }
        }
        return result;
    }

    public static ArrayList<ArrayList<Integer>> String2Array(String str) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        String txt = str.substring(str.indexOf("{") + 1, str.indexOf("}"));
        String[] strArray = txt.split(",,");
        for (int i = 0; i < strArray.length; i++) {
            String temp = strArray[i];
            result.add(String2ListInteger(temp));
        }
        return result;
    }

    public static int string2Integer(String str) {
        try {
            int result = 0;
            if (str.startsWith("0x") || str.startsWith("0X")) {
                result = Integer.parseInt(str.substring(2), 16);
            } else {
                result = Integer.parseInt(str);
            }
            return result;
        } catch (Exception e) {
            return 0;
        }
    }
}
