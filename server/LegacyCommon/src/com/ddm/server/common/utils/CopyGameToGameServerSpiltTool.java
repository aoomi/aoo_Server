package com.ddm.server.common.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @auto leo_wi
 * @description 复制游戏到分服 注意：：：：： OpPointEnum  OpType 有新增要自己手动加
 * @date 2020/12/10
 */
public class CopyGameToGameServerSpiltTool {

    /**
     * 是否覆盖游戏,T:会删掉原有的包
     */
    public static final Boolean COVER_MODULE = true;
    /**
     * 是否麻将游戏
     */
    public static final Boolean MJ = true;
    /**
     * 要复制的游戏名称
     */
    public static final String TARGET_GAME_NAME = "FYDDZMJ";
    /**
     * game_server 路径
     */
    public static final String GAME_SERVER_PATH = "D:\\Workship\\Cocos\\Game258\\tag\\HallAndSubGame\\Server_game";
    /**
     * game_server_spilt 路径
     */
    public static final String GAME_SERVER_SPILT_PATH = "D:\\Workship\\Cocos\\Game258\\tag\\HallAndSubGame\\Server_game_split";

    /**
     * 复制的consumer包 的源路径,这里可以不用改
     */
    public static final String Consumer_RESOURCE_NAME = "GYMJ";

    public static void main(String[] args) {
        //获取项目路径

        try {
            CopyGameServerToSplit();
            CopyConsumerFiels();
            CopyShenPai();
        } catch (Exception e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
    }


    private static void CopyConsumerFiels() throws Exception {

        // 复制目标文件
        String resourceModuleName = GAME_SERVER_SPILT_PATH + File.separator + Consumer_RESOURCE_NAME + "\\src\\business\\rocketmq\\" + Consumer_RESOURCE_NAME.toLowerCase() + "\\consumer";
        //黏贴目标文件
        String pastModuleName = GAME_SERVER_SPILT_PATH + File.separator + TARGET_GAME_NAME + "\\src\\business\\rocketmq\\" + TARGET_GAME_NAME.toLowerCase() + "\\consumer";
        File resourceFile = new File(GAME_SERVER_PATH);
        if (!resourceFile.exists()) {
            throw new Exception("源目标路径：[" + GAME_SERVER_PATH + "] 不存在...");
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
            targetFile.mkdirs();
        }

        //先从 gameServer 复制
        copyFolder(resourceModuleName, pastModuleName, true);
    }


    /**
     * 复制game_server 到game_server_split
     */
    public static void CopyGameServerToSplit() throws Exception {

        // 复制目标文件
        String resourceModuleName = GAME_SERVER_PATH + File.separator + TARGET_GAME_NAME;
        //黏贴目标文件
        String pastModuleName = GAME_SERVER_SPILT_PATH + File.separator + TARGET_GAME_NAME;
        File game_server = new File(GAME_SERVER_PATH);
        if (!game_server.exists()) {
            throw new Exception("源目标路径：[" + GAME_SERVER_PATH + "] 不存在...");
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

        //先从 gameServer 复制
        copyFolder(resourceModuleName, pastModuleName, false);


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
    public static void copyFolder(String resource, String target, boolean changeName) throws Exception {

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
            // 复制文件
            if (file.isFile()) {
                String fileTyle = fileName.substring(fileName.lastIndexOf("."));

                if (fileTyle.equals(".iml") || fileTyle.equals(".DS_Store")) {
                    System.err.println(fileName + "文件不复制 ");
                    continue;
                }
                System.out.print("源文件:" + file.getName());
                if (changeName) {
                    fileName = fileName.replaceAll(Consumer_RESOURCE_NAME, TARGET_GAME_NAME);
                    //替换类名
                    fileName = fileName.replaceAll(Consumer_RESOURCE_NAME.toLowerCase(), TARGET_GAME_NAME.toLowerCase());
                }

                System.out.print("\t目标文件:" + fileName + "\n");

                // 在 目标文件夹（B） 中 新建 源文件夹（A），然后将文件复制到 A 中
                File targetFile1 = new File(target + File.separator + fileName);
                if (!targetFile1.exists()) {
                    targetFile1.createNewFile();
                }
                copyFile(file, targetFile1, changeName);
            }
            // 复制文件夹
            if (file.isDirectory()) {
                //bin不复制
                if (fileName.equals("bin")) {
                    continue;
                }
                File file1 = new File(target + File.separator + fileName);
                if (!file1.exists()) {
                    file1.mkdir();
                }
                // 目的文件夹
                String dir1 = file.getAbsolutePath();
                String dir2 = file1.getAbsolutePath();
                copyFolder(dir1, dir2, changeName);
            }
        }

    }

    /**
     * 复制文件
     *
     * @param resource
     * @param target
     */
    public static void copyFile(File resource, File target, boolean changeName) throws Exception {
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
        boolean roomFile = target.getName().equals(TARGET_GAME_NAME + "Room.java");
        while ((line = br.readLine()) != null) {
            if (roomFile) {
                if (line.contains(" super(baseRoomConfigure, roomKey, ownerID);")) {
                    line += "\t\n" + "        initShareBaseCreateRoom(C" + TARGET_GAME_NAME + "_CreateRoom.class,baseRoomConfigure);;";
                }
                if (line.contains("if (this.cfg == null) {")) {
                    line += "\t\n" + "\t\t\tinitShareBaseCreateRoom(C" + TARGET_GAME_NAME + "_CreateRoom.class,getBaseRoomConfigure());";
                }
            } else if (changeName) {
                if (line.contains(Consumer_RESOURCE_NAME.toLowerCase())) {
                    line = line.replaceAll(Consumer_RESOURCE_NAME.toLowerCase(), TARGET_GAME_NAME.toLowerCase());
                }
                if (line.contains(Consumer_RESOURCE_NAME)) {
                    line = line.replaceAll(Consumer_RESOURCE_NAME, TARGET_GAME_NAME);
                }
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


    private static void CopyShenPai() throws Exception {
        String resPath = GAME_SERVER_PATH + "\\gameServer\\conf\\" + (MJ ? "MJ" : "PK") + TARGET_GAME_NAME + "Config.txt";
        String tarPath = GAME_SERVER_SPILT_PATH + "\\gameServer\\conf\\" + (MJ ? "MJ" : "PK") + TARGET_GAME_NAME + "Config.txt";
        File resource = new File(resPath);
        File target = new File(tarPath);
        if (target.exists() || !resource.exists()) {
            return;
        }
        target.createNewFile();
        copyFile(resource, target, false);
    }
}
