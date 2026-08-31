package com.ddm.server.common.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @auto leo_wi
 * @description 赋值游戏
 * @date 2020/5/7 上午11:07
 */
public class CopyModuleTool {

    /**
     * 是否覆盖游戏,T:会删掉原有的包
     */
    public static final Boolean COVER_MODULE = false;

    /**
     * 是否麻将游戏
     */
    public static final Boolean MJ = true;
    public static final String RESOURCE_NAME = "JDZTS";
    public static final String TARGET_NAME = "DD";
    public static final String RESOURCE_LOWER_NAME = "jdzts";
    public static final String TARGET_LOWER_NAME = "dd";
    public static final String RESOURCE_CH_NAME = "景德镇讨赏";
    public static final String TARGET_CH_NAME = "打盾";
    public static final int RESOURCE_GAME_ID = 171;
    public static final int TARGET_GAME_ID = 176;
    public static final String PATH = "D:\\tag\\HallAndSubGame\\Server_game_split";
    public static final String CONFIG_PATH = "D:\\tag\\HallAndSubGame\\Server_game_split\\gameServer\\conf\\";

    public static void CopyModule() throws Exception {


        // 复制目标文件
        String resourceModuleName = PATH + File.separator + RESOURCE_NAME;
        //黏贴目标文件
        String pastModuleName = PATH + File.separator + TARGET_NAME;
        File h5_qipai = new File(PATH);
        if (!h5_qipai.exists()) {
            throw new Exception("源目标路径：[" + PATH + "] 不存在...");
        }
        File resourceFile = new File(resourceModuleName);
        if (!resourceFile.exists()) {
            throw new Exception("源目标路径：[" + resourceModuleName + "] 不存在...");
        }
        File targetFile = new File(pastModuleName);
        if (targetFile.exists() && !COVER_MODULE) {
            throw new Exception("存放的目标路径：[" + pastModuleName + "] 已经存在...");
        }
        //如果要覆盖
        if (COVER_MODULE && targetFile.exists()) {
            deleteFolder(targetFile);
        }
        //如果不存在 创建
        if (!targetFile.exists()) {
            targetFile.mkdir();
        }

        //复制文件/文件夹
        copyFolder(resourceModuleName, pastModuleName);

    }

    /**
     * 递归删除
     *
     * @param targetFile
     */
    public static void deleteFolder(File targetFile) throws Exception {
        if (!targetFile.exists()) {
            throw new Exception("文件：[" + targetFile.getName() + "]不存在...");
        }
        if (targetFile.isFile()) {
            System.out.println("删除文件 " + targetFile.getName());

            targetFile.delete();
        } else {
            File[] files = targetFile.listFiles();
            for (File file : files) {
                deleteFolder(file);
            }
            System.out.println("删除文件夹 " + targetFile.getName());

            targetFile.delete();
        }
    }

    /**
     * 复制文件夹
     *
     * @param resource 源路径
     * @param target   目标路径
     */
    public static void copyFolder(String resource, String target) throws Exception {

        File resourceFile = new File(resource);
        if (!resourceFile.exists()) {
            throw new Exception("源目标路径：[" + resource + "] 不存在...");
        }
        File targetFile = new File(target);
        if (!targetFile.exists()) {
            throw new Exception("存放的目标路径：[" + target + "] 不存在...");
        }

        // 获取源文件夹下的文件夹或文件
        File[] resourceFiles = resourceFile.listFiles();
        for (File file : resourceFiles) {
            String fileName = file.getName();
            if (fileName.equals("target")) {
                System.err.println("target文件夹不复制 ");
                continue;
            }
            if (fileName.equals("bin")) {
                System.err.println("bin文件夹不复制 ");
                continue;
            }
            // 复制文件
            if (file.isFile()) {
                String fileTyle = fileName.substring(fileName.lastIndexOf("."));

                if (fileTyle.equals(".iml") || fileTyle.equals(".DS_Store")) {
                    System.err.println(fileName + "文件不复制 ");
                    continue;
                }
                System.out.print("源文件:" + file.getName());
                //替换类名
                fileName = fileName.replaceAll(RESOURCE_NAME, TARGET_NAME);
                //替换类名
                fileName = fileName.replaceAll(RESOURCE_LOWER_NAME, TARGET_LOWER_NAME);

                System.out.print("\t目标文件:" + fileName + "\n");

                // 在 目标文件夹（B） 中 新建 源文件夹（A），然后将文件复制到 A 中
                File targetFile1 = new File(target + File.separator + fileName);
                if (!targetFile1.exists()) {
                    targetFile1.createNewFile();
                }
                copyFile(file, targetFile1);
            }
            // 复制文件夹
            if (file.isDirectory()) {
                //替换包名
                fileName = fileName.replaceAll(RESOURCE_LOWER_NAME, TARGET_LOWER_NAME);
                File file1 = new File(target + File.separator + fileName);
                if (!file1.exists()) {
                    file1.mkdir();
                }
                // 目的文件夹
                String dir1 = file.getAbsolutePath();
                String dir2 = file1.getAbsolutePath();
                copyFolder(dir1, dir2);
            }
        }

    }

    /**
     * 复制文件
     *
     * @param resource
     * @param target
     */
    public static void copyFile(File resource, File target) throws Exception {
        // 输入流 --> 从一个目标读取数据
        // 输出流 --> 向一个目标写入数据

        long start = System.currentTimeMillis();

        // 文件输入流并进行缓冲
        FileInputStream inputStream = new FileInputStream(resource);
        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);

        // 文件输出流并进行缓冲
        FileOutputStream outputStream = new FileOutputStream(target);
        OutputStreamWriter osw = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(osw);
        String line = "";
        String classLine = "";
        String[] arrs = null;
        while ((line = br.readLine()) != null) {
            if (line.contains(RESOURCE_LOWER_NAME)) {
                line = line.replaceAll(RESOURCE_LOWER_NAME, TARGET_LOWER_NAME);
            }
            if (line.contains(RESOURCE_NAME)) {
                line = line.replaceAll(RESOURCE_NAME, TARGET_NAME);
            }
            if (line.contains(RESOURCE_CH_NAME)) {
                line = line.replaceAll(RESOURCE_CH_NAME, TARGET_CH_NAME);
            }
            if (line.contains(" int gameTypeId = " + RESOURCE_GAME_ID)) {
                line = line.replaceAll("" + RESOURCE_GAME_ID, "" + TARGET_GAME_ID);
            }
            bw.write(line + "\t\n");
        }
        br.close();
        isr.close();
        inputStream.close();
        bw.close();
        osw.close();
        outputStream.close();

        long end = System.currentTimeMillis();

//        System.out.println("耗时：" + (end - start) / 1000 + " s");

    }


    public static void main(String[] args) {
        //获取项目路径

        try {
            CopyModule();
            CopyShenPai();
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
    }

    private static void CopyShenPai() throws Exception {
        String resPath = CONFIG_PATH + (MJ ? "MJ" : "PK") + RESOURCE_NAME + "Config.txt";
        String tarPath = CONFIG_PATH + (MJ ? "MJ" : "PK") + TARGET_NAME + "Config.txt";
        File resource = new File(resPath);
        File target = new File(tarPath);
        if (target.exists() || !resource.exists()) {
            return;
        }
        target.createNewFile();
        copyFile(resource, target);
    }
}
