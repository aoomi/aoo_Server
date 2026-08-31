package com.ddm.server.common.utils;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * sql数据报错还原工具
 *
 * @author zhujianming
 * @date 2020-11-10 18:04
 */
public class SqlDataUtil {

    private final static SqlDataUtil instance = new SqlDataUtil();

    private static SqlDataUtil getInstance() {
        return instance;
    }

    public static void main(String args[]) {
        //从线上日志截取失败的数据，存到新文件
        getInstance().getLogByBeginStringAndEndString("D:\\Downloads\\all_2020-11-10.log", "The total number of locks exceeds the lock table size Query:", "(org.apache.commons.dbutils.AbstractQueryRunner");
        //根据数据表分别存储sql语句
        getInstance().splitSqlToFile("D:\\Downloads\\log\\all_2020-11-10.log", "D:\\Downloads\\log\\");
        //替代？参数，存到原来的文件
        getInstance().reductionParameters("D:\\Downloads\\log\\ClubLevelRoomCountLog.log");
    }

    /**
     * 获取日志开始字符串和字符串结束
     * 获取日志(通过开始字符串和结束字符串)
     *
     * @param path  路径 例：D:\Downloads\all_2020-11-10.log
     * @param begin 开始 例：The total number of locks exceeds the lock table size Query:
     * @param end   结束 例：(org.apache.commons.dbutils.AbstractQueryRunner
     */
    public void getLogByBeginStringAndEndString(String path, String begin, String end) {
        File file = new File(path);
        //取文件
        if (file.exists()) {
            try {
                //读文件
                BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8));
                PrintWriter printWriter = null;
                try {
                    String line;
                    StringBuilder buf = new StringBuilder();
                    //单行读取
                    while ((line = br.readLine()) != null) {
                        //修改内容核心代码
                        int beginPos = line.indexOf(begin);
                        int endPos = line.indexOf(end);
                        if (beginPos >= 0 && endPos >= 0) {
                            //截取所需字符串
                            String targetString = line.substring(beginPos + begin.length(), endPos);
                            System.out.println(targetString.trim());
                            buf.append(targetString.trim());
                            buf.append(System.getProperty("line.separator"));//行与行之间的分割
                        }
                    }
                    //开始写入文件，覆盖方式
                    if (buf.length() > 0) {
                        printWriter = new PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8);
                        printWriter.write(buf.toString().toCharArray());
                        printWriter.flush();
                    }
                } finally {
                    br.close();
                    if (printWriter != null) {
                        printWriter.close();
                    }
                }
            } catch (Exception e) {
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            }
        }
    }

    /**
     * 还原参数
     *
     * @param path 路径 例：D:\Downloads\log\ClubLevelRoomCountLog.log
     */
    public void reductionParameters(String path) {
        File file = new File(path);
        //读取文件
        if (file.exists()) {
            try {
                //读取单行内容
                BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8));
                PrintWriter printWriter = null;
                try {
                    String line;
                    StringBuilder buf = new StringBuilder();
                    //逐行读取
                    while ((line = br.readLine()) != null) {
                        //根据参数切分数据
                        String splitPoint = "Parameters:";
                        //参数起点
                        int parameterBeginPos = line.indexOf(splitPoint);
                        if (parameterBeginPos >= 0) {
                            //如果是插入语句
                            if (line.startsWith("insert into") || line.startsWith("INSERT INTO")) {
                                //截取参数字符串
                                String parameterString = line.substring(parameterBeginPos + splitPoint.length());
                                //参数字符串去除头尾的[]
                                parameterString = parameterString.substring(parameterString.indexOf("[") + 1, parameterString.lastIndexOf("]"));
                                if (parameterString.endsWith("]")) {
                                    //如果是二维数组，去除尾巴第二个]
                                    parameterString = parameterString.substring(0, parameterString.lastIndexOf("]"));
                                }
                                //按参数数组切割批处理参数（针对二维数组参数）
                                String[] parameterArray = parameterString.split("], \\[");
                                //合并sql头
                                String preString = line.substring(0, line.indexOf("values")) + "values";
//                              //批处理参数处理
                                for (String parameterItem : parameterArray) {
                                    if (parameterItem.startsWith("[")) {
                                        //如果是二维数组，去除头部[
                                        parameterItem = parameterItem.substring(parameterString.indexOf("[") + 1);
                                    }
                                    String[] parameterPasteSpace = (parameterItem + " ").split(", ");
                                    // 遍历sql参数语句
                                    StringBuffer value = new StringBuffer();
                                    for (String itemSqlParameter : parameterPasteSpace) {
                                        itemSqlParameter = itemSqlParameter.trim();
                                        //如果语句结尾是,说明有空参数(需要把空参数也加入)
                                        if (itemSqlParameter.endsWith(",")) {
                                            //加入结尾的第一个参数
                                            itemSqlParameter = itemSqlParameter.substring(0, itemSqlParameter.length() - 1);
                                            if (NumberUtils.isCreatable(itemSqlParameter.trim())) {
                                                value.append(itemSqlParameter + ",");
                                            } else {
                                                value.append("'" + itemSqlParameter + "'" + ",");
                                            }
                                            //加入结尾的空参数
                                            value.append("''" + ",");
                                        } else {
                                            //加入参数
                                            if (NumberUtils.isCreatable(itemSqlParameter.trim())) {
                                                value.append(itemSqlParameter + ",");
                                            } else {
                                                value.append("'" + itemSqlParameter + "'" + ",");
                                            }
                                        }
                                    }
                                    //去除结尾的，并加入(),
                                    preString = preString + "(" + value.toString().substring(0, value.length() - 1) + "),";
                                }
                                //去除结尾的,
                                preString = preString.substring(0, preString.length() - 1);
                                System.out.println(preString);
                                //加入每行的分号
                                buf.append(preString.trim() + ";");
                                //行与行之间的分割
                                buf.append(System.getProperty("line.separator"));
                            } else if (line.startsWith("update")) {//更新
                                //截取参数字符串
                                String parameterString = line.substring(parameterBeginPos + splitPoint.length());
                                //参数字符串去除头尾的[]
                                parameterString = parameterString.substring(parameterString.indexOf("[") + 1, parameterString.lastIndexOf("]") - 1);
                                //合并sql头
                                String preString = line.substring(0, line.indexOf("Parameters"));
                                String[] parameterPasteSpace = parameterString.split(", ");
                                // 参数处理
                                for (String item : parameterPasteSpace) {
                                    item = item.trim();
                                    if (item.endsWith(",")) {
                                        //如果语句结尾是,说明有空参数(需要把空参数也加入)
                                        item = item.substring(0, item.length() - 1);
                                        //参数替换?
                                        if (NumberUtils.isCreatable(item.trim())) {
                                            preString = replaceFirst(preString, "?", item);
                                        } else {
                                            preString = replaceFirst(preString, "?", "'" + item + "'");
                                        }
                                        preString = replaceFirst(preString, "?", "''");
                                    } else {
                                        //参数替换?
                                        if (NumberUtils.isCreatable(item.trim())) {
                                            preString = replaceFirst(preString, "?", item);
                                        } else {
                                            preString = replaceFirst(preString, "?", "'" + item + "'");
                                        }
                                    }
                                }
                                //行语句还原
                                System.out.println(preString);
                                buf.append(preString.trim() + ";");
                                buf.append(System.getProperty("line.separator"));//行与行之间的分割
                            }
                        }
                    }
                    if (buf.length() > 0) {
                        printWriter = new PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8);//替换后输出的文件位置（切记这里的E:/ttt 在你的本地必须有这个文件夹）
                        printWriter.write(buf.toString().toCharArray());
                        printWriter.flush();
                    }
                } finally {
                    br.close();
                    if (printWriter != null) {
                        printWriter.close();
                    }
                }
            } catch (Exception e) {
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            }
        }
    }

    /**
     * 取代
     *
     * @param source  源
     * @param regix   regix
     * @param replace 取代
     * @return {@link String}
     */
    public String replaceFirst(String source, String regix, String replace) {
        int i = source.indexOf(regix);
        if (i >= 0) {
            String pre = source.substring(0, i);
            String end = source.substring(i + 1);
            return pre + replace + end;
        }
        return source;
    }


    /**
     * sql文件分割
     *
     * @param path       路径 例：D:\Downloads\log\all_2020-11-10.log
     * @param targetPath 目标路径 例: D:\Downloads\log\
     */
    public void splitSqlToFile(String path, String targetPath) {
        File file = new File(path);
        //多文件写工具
        Map<String, FileWriter> printer = new HashMap<>();
        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8));
                PrintWriter printWriter = null;
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String fileName = "";
                        if (line.startsWith("insert into")) {
                            fileName = line.substring(line.indexOf("insert into") + "insert into".length(), line.indexOf("("));
                        } else if (line.startsWith("INSERT INTO")) {
                            fileName = line.substring(line.indexOf("INSERT INTO") + "INSERT INTO".length(), line.indexOf("("));
                        } else if (line.startsWith("update")) {
                            fileName = line.substring(line.indexOf("update") + "update".length(), line.indexOf("set"));
                        }
                        fileName = fileName.replace("`", "").trim();
                        if (!printer.containsKey(fileName)) {
                            File newFile = new File(targetPath + fileName + ".log");
                            newFile.createNewFile();
                            // 使用FileWriter写文件
                            FileWriter writer = null;
                            try {
                                writer = new FileWriter(newFile, java.nio.charset.StandardCharsets.UTF_8);
                                writer.append(line);
                                writer.append(System.getProperty("line.separator"));//行与行之间的分割
                                writer.flush();
                            } catch (Exception e) {
                                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                            }
                            printer.put(fileName, writer);
                        } else {
                            FileWriter writer = printer.get(fileName);
                            try {
                                writer.append(line);
                                writer.append(System.getProperty("line.separator"));//行与行之间的分割
                                writer.flush();
                            } catch (Exception e) {
                                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                            }
                        }
                    }
                } finally {
                    if (br != null) {
                        br.close();
                    }
                    if (printWriter != null) {
                        printWriter.close();
                    }
                    for (FileWriter writer : printer.values()) {
                        if (writer != null) {
                            writer.close();
                        }
                    }
                }
            } catch (Exception e) {
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            }
        }
    }
}
