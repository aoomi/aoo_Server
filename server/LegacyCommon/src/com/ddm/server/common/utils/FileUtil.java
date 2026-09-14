package com.ddm.server.common.utils;

import com.ddm.server.common.CommLogD;
import com.google.common.io.Files;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件创建类
 */
public class FileUtil {

    /**
     * 源项目
     *
     * @return
     */
    public String getSource() {
        return "lpmj";
    }

    /**
     * 新项目
     *
     * @return
     */
    public String getReplace() {
        return "jsmj";
    }

    //文件替换映射
    public Map<String, String> fileNamePreMap = new HashMap() {{
        put("C" + getSource().toUpperCase(), "C" + getReplace().toUpperCase());
        put("S" + getSource().toUpperCase(), "S" + getReplace().toUpperCase());
        put(getSource().toUpperCase(), getReplace().toUpperCase());
    }};

    /**
     * 创建类型
     */
    public enum CreateType {
        File(1),
        Directory(2);
        final int value;

        CreateType(int value) {
            this.value = value;
        }
    }


    /**
     * 获得文件名忽略后缀
     *
     * @param file 文件
     * @return {@link String}
     */
    public String getFileNameWithoutSuffix(File file) {
        //忽略判断
        String fileName = file.getName();
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * 拷贝文件
     *
     * @param file1 file1
     * @param file2 file2
     */
    public void fileCopy(File file1, File file2) {
        File[] files1 = file1.listFiles();
        AssertsUtil.check(files1 != null, "listFile is null");
        for (int i = 0; i < files1.length; i++) {
            if (files1[i].isDirectory()) {
                String s = files1[i].getName();
                File file = new File(file2.getPath() + "/" + s);
                file.mkdir();
                fileCopy(files1[i], file);
            } else if (files1[i].isFile()) {
                String s = files1[i].getName();
                BufferedReader br = null;
                BufferedWriter bw = null;
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(files1[i].getPath() + ""), java.nio.charset.StandardCharsets.UTF_8));
                    File newFile = new File(file2.getPath() + "/" + s);
                    if (!newFile.exists()) {
                        Files.createParentDirs(newFile);
                        newFile.createNewFile();
                    }
                    bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile), java.nio.charset.StandardCharsets.UTF_8));
                    String t;
                    while ((t = br.readLine()) != null) bw.write(t);

                } catch (Exception e) {
                    System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                        }
                    }
                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e) {
                            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 根据Map替换内容
     *
     * @param targetFile     目标文件
     * @param replaceTextMap 替换文本映射
     */
    public void replaceMapContent(File targetFile, Map<String, String> replaceTextMap) {
        BufferedReader bufferedReader = null;
        PrintWriter printWriter = null;
        try {
            try {
                bufferedReader = new BufferedReader(new FileReader(targetFile, java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String str;
                while ((str = bufferedReader.readLine()) != null) {
                    boolean isContant = false;
                    for (Map.Entry<String, String> item : replaceTextMap.entrySet()) {
                        if (str.contains(item.getKey())) {
                            String replaceText = str.replace(item.getKey(), item.getValue());
                            sb.append(replaceText);
                            sb.append(System.getProperty("line.separator"));
                            isContant = true;
                            break;
                        }
                    }
                    if (!isContant) {
                        sb.append(str);
                        sb.append(System.getProperty("line.separator"));
                    }
                }
                if (sb.length() != 0) {
                    printWriter = new PrintWriter(targetFile, java.nio.charset.StandardCharsets.UTF_8);
                    printWriter.write(sb.toString().toCharArray());
                    printWriter.flush();
                }
            } catch (Exception e) {
                CommLogD.error("File operation failed", e);
            } finally {
                if (printWriter != null) {
                    printWriter.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
        } catch (Exception e) {
                CommLogD.error("File operation failed", e);
        }
    }

    /**
     * 复制文件
     *
     * @param oldPath 源文件
     * @param newPath 目标文件
     */
    public void copyFile(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        File file = new File(newPath);
        try {
            if (!file.exists() || !file.isFile()) {
                try (FileInputStream in = new FileInputStream(oldFile); FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                } catch (Exception e) {
            CommLogD.error("File copy failed");
                }
            }
        } catch (Exception e) {
            CommLogD.error("copyFile：" + e.getMessage());
        }
    }

    /**
     * 打印文字
     *
     * @param testWord
     */
    public static void printTestWord(String testWord) {
        CommLogD.debug(testWord);
    }

}
