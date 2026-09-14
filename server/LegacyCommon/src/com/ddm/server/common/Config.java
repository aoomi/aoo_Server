package com.ddm.server.common;

import com.ddm.server.common.utils.Enums.LogEnum;


public class Config {

    public static int GameID() {
        return 3;// 游戏ID狠西游游戏ID
    }

    public static String getPlatform() {
        return System.getProperty("Platform", "");
    }

    // ------------------------- 服务器ID配置 -------------------------
    private static Integer serverIid = null;

    /**
     * @return 服务器整形ID
     */
    public static int ServerID() {
        if (serverIid == null) {
            serverIid = Integer.getInteger("ServerID_Int");
            if (serverIid == null) {
                throw new UnsupportedOperationException("ServerID_Int 不能为空");
            }
        }
        return serverIid;
    }

    private static String serverSid = null;

    /**
     * @return 服务器字符串ID
     */
    public static String ServerIDStr() {
        if (serverSid == null) {
            serverSid = System.getProperty("ServerID_Str");
            if (serverSid == null || serverSid.trim().isEmpty()) {
                throw new UnsupportedOperationException("ServerID_Str 不能为空");
            }
        }
        return serverSid;
    }

    // ---------------------------------------------------------------

    private static long startTime = 0;

    /**
     * @return 本次服务器开服时间 单位：秒
     */
    public static long getServerStartTime() {
        return startTime;
    }

    /**
     * 设置开服时间
     *
     * @param starttime
     */
    static void setStartTime(long starttime) {
        startTime = starttime;
    }

    /***
     * 数据库主键起始ID
     *
     * @return
     */
    public static long getInitialID() {
        return ServerID() * 100000L;
    }

    /**
     * 心跳地址
     *
     * @return
     */
    public static String GmHeartBeatAddr() {
        String baseurl = System.getProperty("downConfUrl");
        if (baseurl == null || baseurl.isEmpty()) {
            return null;
        }
        return System.getProperty("GmHeartBeatAddr", "http://" + baseurl + "/api.php");
    }

    /**
     * 获取跑马灯通知地址
     *
     * @return
     */
    public static String PhpMargueeNoticeAddr() {
        return System.getProperty("PhpMargueeNoticeAddr");
    }

    /**
     * 获取礼包码兑换地址
     *
     * @return
     */
    public static String PhpExchangeCodeAddr() {
        return System.getProperty("PhpExchangeCodeAddr");
    }

    public static String ServerKey() {
        return System.getProperty("Server_Key");
    }

    public static boolean isOpenLog(LogEnum logEnum) {
        String Open = "1";
        try {
            switch (logEnum) {
                case Debug:
                    return System.getProperty("Log_Debug").equals(Open);
                case Error:
                    return System.getProperty("Log_Error").equals(Open);
                case Info:
                    return System.getProperty("Log_Info").equals(Open);
                case Warn:
                    return System.getProperty("Log_Warn").equals(Open);
            }
        } catch (Exception exception) { /* exception-policy: missing optional logging property disables that level */ }
        return false;
    }

    public static boolean DE_DEBUG() {
        String deDEBUG = System.getProperty("DE_DEBUG");
        return Integer.parseInt(deDEBUG) == 1 ? true : false;
    }

    public static boolean DE_DEBUG_ROOM() {
        String deDEBUG = System.getProperty("DE_DEBUG_ROOM");
        return Integer.parseInt(deDEBUG) == 1 ? true : false;
    }


    /*
     * 机器人头像地址
     * */
    public static String getRobotHeadImageUrl() {
        return System.getProperty("RobotHeadImageUrl");
    }


    /**
     * 回放文件存放地址
     *
     * @return
     */
    public static String PlayBackFilePath() {
        return System.getProperty("PlayBackFilePath", "../bin");
    }


    /**
     * 检查是否新版本包
     *
     * @return
     */
    public static boolean CheckVersionName() {
        return "NEW_HALL_GAME".equals(System.getProperty("VersionName"));
    }

    /**
     * 是否启动共享
     *
     * @return
     */
    public static boolean isShare() {
        String isShare = System.getProperty("isShare");
        return Integer.parseInt(isShare) == 1 ? true : false;
    }

    /**
     * 节点名称
     *
     * @return
     */
    public static String nodeName() {
        return System.getProperty("nodeName");
    }

    /**
     * 节点地址
     *
     * @return
     */
    public static String nodeVipAddress() {
        return System.getProperty("nodeVipAddress");
    }

    /**
     * 节点ip
     *
     * @return
     */
    public static String nodeIp() {
        return System.getProperty("nodeIp");
    }

    /**
     * 节点端口
     *
     * @return
     */
    public static Integer nodePort() {
        return Integer.parseInt(System.getProperty("nodePort"));
    }

    /**
     * 重启等待时间
     *
     * @return
     */
    public static Integer restartServerWaitTime() {
        return Integer.parseInt(System.getProperty("restartServerWaitTime"));
    }

    /**
     * 是否重启踢出玩家
     *
     * @return
     */
    public static boolean isKickPlayer() {
        String isKickPlayer = System.getProperty("isKickPlayer");
        return Integer.parseInt(isKickPlayer) == 1 ? true : false;
    }

    /**
     * 是否等待玩家退出
     *
     * @return
     */
    public static boolean isWaitPlayerOut() {
        String isWaitPlayerOut = System.getProperty("isWaitPlayerOut");
        return Integer.parseInt(isWaitPlayerOut) == 1 ? true : false;
    }

    /**
     * 备用节点端口
     *
     * @return
     */
    public static Integer backUpNodePort() {
        return Integer.parseInt(System.getProperty("backUpNodePort"));
    }

    /**
     * 是否切换备用端口
     *
     * @return
     */
    public static boolean isStartChangePort() {
        String isWaitPlayerOut = System.getProperty("isStartChangePort");
        return Integer.parseInt(isWaitPlayerOut) == 1 ? true : false;
    }

//	/**
//	 * 初始化缓存数据
//	 * @return
//	 */
//	public static boolean shareDataInit() {
//		String isWaitPlayerOut = System.getProperty("shareDataInit");
//		return Integer.parseInt(isWaitPlayerOut) == 1 ? true : false;
//	}

    /**
     * 是否启动共享本地缓存
     *
     * @return
     */
    public static boolean isShareLocal() {
        String isShare = System.getProperty("isShareLocal");
        return Integer.parseInt(isShare) == 1 ? true : false;
    }

    public final static String getTokenSecret() {
        return System.getProperty("aes.secret");
    }

    public final static String getPublicKey() {
        return System.getProperty("rsa.publicKey");
    }

    public final static String getPrivateKey() {
        return System.getProperty("rsa.privateKey");
    }

    public final static String getLocalServer() {
        return "LOCAL_SERVER";
    }

    public final static String getLocalServerTopic() {
        return Config.ServerIDStr().toUpperCase() + "-TOPIC-" + Config.ServerID();
    }


    /**
     * 本地服务
     */
    public static final String LOCAL_SERVER = "LOCAL_SERVER";

    /**
     * 注册中心通知
     */
    public static final String REGISTRY_NOTIFY = "REGISTRY_NOTIFY";

    /**
     * 服务间通知
     */
    public static final String SERVER_NOTIFY = "SERVER_NOTIFY";

    /**
     * 获取ip白名单
     *
     * @return
     */
    public static String getAllowRequestedIP() {
        return System.getProperty("AllowRequestedIP", "[127.0.0.1]");
    }
}
