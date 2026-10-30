package com.ddm.server.common;

import BaseCommon.CommLog;
import BaseServer.BaseServerInit;
import com.ddm.server.common.utils.CommProperties;
import com.ddm.server.loader.JarLoaderMgr;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public abstract class IApp {

    public void init(String[] args) throws Exception {
        //String configfile =System.getProperty("../bin/conf") ;
        String configfile = System.getProperty("user.dir") + "/common/bin/conf";
        CommLogD.info("服务器启动参数数量:{}", args == null ? 0 : args.length);
        printBanner();
        if (args != null && args.length > 0) {
            configfile = args[0];
        }

        // 初始化最基础配置：服务器的配置文件路径，logback配置路径
        this.beforeInit(configfile);
        // 输出git记录
        this.outputVersion();
        // 初始化服务器及其相关任务线程等
        BaseServerInit.initBaseServer();
        // 检查版本标识
        this.checkVersionName();
        // 执行加载指定目录下jar包----------Windows
        JarLoaderMgr.getInstance().execLoadeJarDirectory(new File(System.getProperty("user.dir") + "/common/lib"));
        //Linux
        //JarLoaderMgr.getInstance().execLoadeJarDirectory(new File(System.getProperty("/bin/conf/lib")));
        if (!initBase()) {
            CommLogD.error("初始化基础服务失败, 退出服务器启动");
            System.exit(-1);
        }
        if (!initLogic()) {
            CommLogD.error("初始化逻辑组建失败, 退出服务器启动");
            System.exit(-1);
        }
        if (!initNetwork()) {
            CommLogD.error("初始化网络组建失败, 退出服务器启动");
            System.exit(-1);
        }

        afterInit();
        CommLogD.info("[服务器启动完毕]!");
    }

    /**
     * 启动底层模块
     *
     * @param configdir
     */
    protected abstract void beforeInit(String configdir);

    /**
     * 基础组建 - 除了基础模块外最先启动的模块
     */
    protected abstract boolean initBase();

    /**
     * 网络组建 - 基础管理器初始化后马上进行初始化
     */
    protected abstract boolean initNetwork();

    /**
     * 逻辑组建 - 在网络组建初始化后初始化
     */
    protected abstract boolean initLogic();

    /**
     * 初始化后的事件
     */
    protected abstract void afterInit();

    /**
     * 服务器启动
     */
    protected abstract void start();

    /**
     * 初始化基础配置
     *
     * @param configdir 配置文件的路径
     * @param logback   logback配置文件
     */
    protected void loadBasicConfig(String configdir, String logback) {
        // 初始化first配置
        String firstdir = configdir + "/first.properties";
        if (!CommProperties.load(firstdir, false)) {
            CommLogD.error("first配置文件[{}]读取失败，服务器退出!", firstdir);
            System.exit(-1);
        }
        String gameConfigdir = configdir + "/game_conf.properties";
        if (!CommProperties.load(gameConfigdir, false)) {
            CommLogD.error("first配置文件[{}]读取失败，服务器退出!", firstdir);
            System.exit(-1);
        }

        // 初始化日志配置
        String logbackdir = configdir + File.separator + logback;
        System.setProperty("logback.configurationFile", logbackdir);
        System.setProperty("logback.configurationFile_bak", logbackdir);
        CommLogD.initLog();

        Config.setStartTime(System.currentTimeMillis());
    }

    /**
     * @param serverType
     * @param serverId
     * @param location
     * @param configfile
     */
    /**
     * @param serverType
     * @param serverId
     * @param location
     * @param configfile
     */
    public void loadRemoteConfig(String serverType, String serverId, String location, String configfile) {
        String url = System.getProperty("downConfUrl");
        if (url != null && !url.trim().isEmpty()) {
            /*
             * url = "http://" + url + "/gm/gmNotice!loadConfig?" + serverType +
             * "=" + serverId; CommLogD.info("从远端[{}]下载配置", url); try {
             * JsonObject root = new JsonParser().parse(new
             * String(HttpUtil.GetAll(url))).getAsJsonObject(); String template
             * = CommFile.bufferedReader(location + "/template/" + configfile);
             * for (Entry<String, String> properties :
             * toMapValues(root).entrySet()) { template =
             * template.replaceAll("\\{" + properties.getKey() + "\\}",
             * properties.getValue()); } if(template.contains("\\{")){
             * CommLogD.info("存在未替换的参数"); } CommFile.Write(location + "/" +
             * configfile, template); CommLogD.info(
             * "download and write config ok"); } catch (Exception e) {
             * CommLogD.error("写入下载来的配置文件失败 ,退出程序,url：[{}],文件:[{}] ", url,
             * configfile, e); System.exit(-1); }
             */
        }
        // 加载下载下来的配置文件
        CommProperties.load(location + "/" + configfile);
    }

    public Map<String, String> toMapValues(JsonObject root) {
        Map<String, String> map = new HashMap<>();
        for (Entry<String, JsonElement> element : root.entrySet()) {
            String name = element.getKey();
            JsonObject jsonObject = element.getValue().getAsJsonObject();
            for (Entry<String, JsonElement> property : jsonObject.entrySet()) {
                JsonElement value = property.getValue();
                if (value != null && !value.isJsonNull()) {
                    map.put(name + "\\." + property.getKey(), value.getAsString());
                }
            }
        }
        return map;
    }

    private void outputVersion() {
        String version = outputVersion("kernel", BaseServerInit.class);
        if (!version.equals(outputVersion("Common", IApp.class))) {
            CommLogD.error("BaseServer的包和Common的Jar包不一致! 运维注意拷贝Server的jar包的时候要拷贝Lib包的内容");
            System.exit(-1);
        }
        if (!version.equals(outputVersion("Server", this.getClass()))) {
            CommLogD.error("BaseServer的包和Server的Jar包不一致! 运维注意拷贝Server的jar包的时候要拷贝Lib包的内容");
            System.exit(-1);
        }
        CommLogD.info("----------------version.txt--------------------");
        for (String line : version.split("\n")) {
            CommLogD.info(line);
        }
        CommLogD.info("-----------------------------------------------");
    }

    /**
     * 检查版本标识
     */
    private void checkVersionName() {
        if (Config.CheckVersionName()) {
            CommLog.info("=================检查到版本标识一致===================");
        } else {
            CommLog.error("=================检查到版本标识不一致，请检查配置表===================");
            System.exit(-1);
        }
    }


    private String outputVersion(String jarPackage, Class<?> rootclass) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream res = rootclass.getResourceAsStream("version.txt")) {
            if (res == null) {
                return "Release Only!";
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(res, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            CommLogD.error("IApp.outputVersion", e);
        }
        return stringBuilder.toString();
    }

    private void printBanner() {
    }
}
