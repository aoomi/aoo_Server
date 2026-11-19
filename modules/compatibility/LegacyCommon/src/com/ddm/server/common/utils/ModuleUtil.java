package com.ddm.server.common.utils;

import com.ddm.server.common.CommLogD;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 复制旧项目生成新项目
 *
 * @author zhujianming
 * @date 2021-07-26 10:29
 */
public class ModuleUtil extends FileUtil {

    private final static ModuleUtil instance = new ModuleUtil();

    private static ModuleUtil getInstance() {
        return instance;
    }

    public String getSource() {
        return "pdk";
    }

    public String getReplace() {
        return "gapdk";
    }

    //新根目录
    private final String sourceSrcAbsolutePath = "G:\\HallAndSubGame\\Server_game_split\\" + getSource().toUpperCase() + "\\src\\";

    public static void main(String args[]) {
        getInstance().generateReplaceModuleFromSource();
    }

//    public static void main(String args[]){
//        //获取该所有目录
//       //循环找到所有文件
//       recursiveDire(new File("G:\\HallAndSubGame\\Server_game_split\\"),1);
//    }

    public static void recursiveDire(File file, int hierarchy) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] listFiles = file.listFiles();
                for (File file1 : listFiles) {
                    if (file1.getAbsolutePath().indexOf(".svn") < 0) {
                        recursiveDire(file1, hierarchy + 1);
                    }
                }
            } else {
                System.out.println("文件：" + file.getAbsolutePath());
                if (file.getName().endsWith(".log") && (file.getName().indexOf("all") >= 0 || file.getName().indexOf("error") >= 0)) {
                    file.delete();
                } else if (file.getName().endsWith(".class")) {
                    file.delete();
                }
            }
        }
    }


    /**
     * 通过旧项目复制
     */
    private void generateReplaceModuleFromSource() {
        //生成新的src文件（替换java文件内package行，并放入新项目中）
        generateTargetSrcFile(sourceSrcAbsolutePath);
        //复制游戏底下的其他文件和文件夹
        File sourceFile = new File(sourceSrcAbsolutePath);
        File[] listFiles = sourceFile.getParentFile().listFiles();
        AssertsUtil.check(listFiles != null, "listFiles is error");
        for (File item : listFiles) {
            String targetPath = item.getAbsolutePath().replace(getSource().toUpperCase(), getReplace().toUpperCase());
            if (item.isDirectory()) {
                //过滤掉src和bin的目录
                if (!item.getName().equals("src") && !item.getName().equals("bin")) {
                    fileCopy(item, new File(targetPath));
                }
            } else {
                //复制文件
                copyFile(item.getAbsolutePath(), targetPath);
                File targetFile = new File(targetPath);
                //当是.project替换下里面项目的东西
                if (item.getName().equals(".project")) {
                    Map<String, String> replaceContent = new HashMap<>();
                    replaceContent.put("<name>" + getSource().toUpperCase() + "</name>", "<name>" + getReplace().toUpperCase() + "</name>");
                    replaceMapContent(targetFile, replaceContent);
                }
            }
        }
    }

    /**
     * 生成新的src文件（替换java文件内package行，并放入新项目中）
     *
     * @param sourceSrcAbsolutePath 源项目src目录
     */
    public void generateTargetSrcFile(String sourceSrcAbsolutePath) {
        File folder = new File(sourceSrcAbsolutePath);
        recursiveDir(folder, sourceSrcAbsolutePath);
    }

    /**
     * 生成新的src文件（替换java文件内package行，并放入新项目中）
     *
     * @param file                  源文件或者文件夹
     * @param sourceSrcAbsolutePath 源项目src目录
     */
    private void recursiveDir(File file, String sourceSrcAbsolutePath) {
        if (file.isDirectory()) {
            //目录递归复制文件并换内容
            File[] fileArray = file.listFiles();
            assert fileArray != null;
            for (File children : fileArray) {
                recursiveDir(children, sourceSrcAbsolutePath);
            }
        } else {
            //获取文件复制生成新文件
            String fileSimpleName = getFileNameWithoutSuffix(file);
            Optional<Map.Entry<String, String>> first = fileNamePreMap.entrySet().stream().filter(n -> fileSimpleName.startsWith(n.getKey())).findFirst();
            if (first.isPresent()) {
                Map.Entry<String, String> matchedPrefix = first.orElseThrow();
                String newFileName = fileSimpleName.replaceFirst(matchedPrefix.getKey(), matchedPrefix.getValue());
                String newFileAbsolutePath = file.getParent().replace(getSource(), getReplace()) + File.separator + newFileName + ".java";
                String packageLineText = getTargetPackage(newFileAbsolutePath, newFileName, sourceSrcAbsolutePath);
                File newFile = new File(newFileAbsolutePath.replace(newFileName + ".java", "").replace(getSource().toUpperCase(), getReplace().toUpperCase()) + newFileName + ".java");
                if (!newFile.exists()) {
                    try {
                        newFile.getParentFile().mkdirs();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    copyFile(file.getAbsolutePath(), newFile.getAbsolutePath());
                    replacePackageAndText(newFile.getAbsolutePath(), packageLineText);
                    System.out.println("重命名文件并替换package和import：" + file.getAbsolutePath() + "->" + newFile.getAbsolutePath());
                }
            } else {
                //不存在匹配的文件，直接替换文件
                if (file.isFile()) {
                    File newFile = new File(file.getParent().replace(getSource(), getReplace()).replace(getSource().toUpperCase(), getReplace().toUpperCase()) + File.separator + fileSimpleName + ".java");
                    if (!newFile.exists()) {
                        try {
                            newFile.getParentFile().mkdirs();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        copyFile(file.getAbsolutePath(), newFile.getAbsolutePath());
                        replacePackageAndText(newFile.getAbsolutePath(), getTargetPackage(file.getAbsolutePath(), fileSimpleName, sourceSrcAbsolutePath));
                        System.out.println("重命名文件并替换package和import：" + file.getAbsolutePath() + "->" + newFile.getAbsolutePath());
                    } else {
                        replacePackageAndText(newFile.getAbsolutePath(), getTargetPackage(file.getAbsolutePath(), fileSimpleName, sourceSrcAbsolutePath));
                        System.out.println("替换package和import：" + "->" + fileSimpleName);
                    }
                }
            }
        }
    }

    /**
     * 替换文件内容
     *
     * @param absolutePath 绝对路径
     * @param packageName  包名
     */
    private void replacePackageAndText(String absolutePath, String packageName) {
        try {
            BufferedReader bufReader = null;//数据流读取文件
            PrintWriter printWriter = null;//替换后输出的文件位置（切记这里的E:/ttt 在你的本地必须有这个文件夹）
            try {
                bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(absolutePath), java.nio.charset.StandardCharsets.UTF_8));//数据流读取文件
                StringBuilder strBuffer = new StringBuilder();
                String temp;
                do {
                    temp = bufReader.readLine();
                    if (temp != null) {
                        Pattern p = Pattern.compile("\\s{0,}package\\s{1,}.*;+");
                        Matcher m = p.matcher(temp);
                        if (m.find()) {
                            strBuffer.append(m.replaceFirst(packageName));
                        } else {
                            strBuffer.append(temp.replace(getSource().toUpperCase(), getReplace().toUpperCase()).replace(getSource().toLowerCase(), getReplace().toLowerCase()));
                        }
                        strBuffer.append(System.getProperty("line.separator"));//行与行之间的分割
                    }
                } while (temp != null);
                printWriter = new PrintWriter(absolutePath, java.nio.charset.StandardCharsets.UTF_8);//替换后输出的文件位置（切记这里的E:/ttt 在你的本地必须有这个文件夹）
                printWriter.write(strBuffer.toString().toCharArray());
                printWriter.flush();
            } catch (Exception e) {
                System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
                System.out.println("Write failed");
            } finally {
                if (bufReader != null) {
                    bufReader.close();
                }
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        } catch (Exception e) {
            CommLogD.error("replacePackageAndText：" + e.getMessage());
        }
    }

    /**
     * 获取目标包package
     *
     * @param fileAbsolutePath
     * @param simpleFileName
     * @param projectRootAbsolutePath
     * @return
     */
    private String getTargetPackage(String fileAbsolutePath, String simpleFileName, String projectRootAbsolutePath) {
        String packageName = "package " + fileAbsolutePath.replace(projectRootAbsolutePath, "").replace("\\", ".").replace(simpleFileName + ".java", "");
        packageName = packageName.substring(0, packageName.length() - 1);
        return packageName + ";";
    }
}
