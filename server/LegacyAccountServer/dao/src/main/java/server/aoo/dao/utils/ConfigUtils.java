package server.aoo.dao.utils;


import com.ddm.server.common.config.CommonConfigInfo;
import com.ddm.server.common.utils.CommLogD;
import server.aoo.dao.config.general.GeneralProperties;

import java.util.HashSet;
import java.util.Set;

public class ConfigUtils {
    private static GeneralProperties config;

    public static void setConfig(GeneralProperties config) {
        ConfigUtils.config = config;
    }

    /**
     * 账号管理服
     *
     * @return
     */
    public final static String getAccountServerUrl() {
        return config.getGeneral().getAccountServerUrl();
    }
    /**
     * 管理服
     *
     * @return
     */
    public final static String getManagerServerUrl() {
        return config.getGeneral().getManagerServerUrl();
    }
    /**
     * 订单管理服
     *
     * @return
     */
    public final static String getOrderServerUrl() {
        return config.getGeneral().getOrderServerUrl();
    }
    /**
     * 资源管理服
     *
     * @return
     */
    public final static String getResServerUrl() {
        return config.getGeneral().getResServerUrl();
    }
    /**
     * 微信管理服
     *
     * @return
     */
    public final static String getWeChatServerUrl() {
        return config.getGeneral().getWeChatServerUrl();
    }
    /**
     * 服务端名称
     *
     * @return
     */
    public final static String getServerName() {
        return config.getGeneral().getServerName();
    }


    /**
     * 微信appId
     *
     * @return
     */
    public final static String getWxAppid() {
        return config.getWechat().getAppid();
    }

    /**
     * 微信秘钥
     *
     * @return
     */
    public final static String getWxSecret() {
        return config.getWechat().getSecret();
    }

    /**
     * 授权类型
     *
     * @return
     */
    public final static String getWxGrantType() {
        return config.getWechat().getGrantType();
    }

    /**
     * 语言类型
     *
     * @return
     */
    public final static String getWxLangType() {
        return config.getWechat().getLangType();
    }

    /**
     * 个人信息授权url
     *
     * @return
     */
    public final static String getWxAccountUserInfoUrl() {
        return config.getWechat().getAccountUserInfoUrl();
    }

    /**
     * 刷新token Url
     *
     * @return
     */
    public final static String getWxRefreshTokenUrl() {
        return config.getWechat().getRefreshTokenUrl();
    }

    /**
     * 获取access Url
     *
     * @return
     */
    public final static String getWxAccessTokenUrl() {
        return config.getWechat().getAccessTokenUrl();
    }

    /**
     * 获取access Url
     *
     * @return
     */
    public final static String getMobileAccessTokenUrl() {
        return config.getMobile().getAccessTokenUrl();
    }


    /**
     * 获取公众号玩家数据
     *
     * @return
     */
    public final static int getGongZhonghaoUserInfoUrl() {
        return Integer.parseInt(config.getWechat().getGongZhonghaoUserInfoUrl());
    }

    /**
     * 创建账号登陆token的加密key
     *
     * @return
     */
    public final static String getTokenSecret() {
        return config.getToken().getTokenSecret();
    }

    /**
     * 创建账号登陆token的加密key
     *
     * @return
     */
    public final static int getTokenExpireTick() {
        return Integer.parseInt(config.getToken().getTokenExpireTick());
    }




    /**
     * 获取日志等级列表
     * @return
     */
    public final static Set<CommLogD.LogEnum> LogLevelSet() {
        Set<CommLogD.LogEnum> logEnumSet = new HashSet<>();
        if (config.getLogLevelD().getWarn() == 1) {
            logEnumSet.add(CommLogD.LogEnum.Warn);
        }
        if (config.getLogLevelD().getDebug() == 1) {
            logEnumSet.add(CommLogD.LogEnum.Debug);
        }
        if (config.getLogLevelD().getError() == 1) {
            logEnumSet.add(CommLogD.LogEnum.Error);
        }
        if (config.getLogLevelD().getInfo() == 1) {
            logEnumSet.add(CommLogD.LogEnum.Info);
        }
        return logEnumSet;
    }


    /**
     *
     * @return
     */
    public final static CommonConfigInfo getCommonConfigInfo() {
        CommonConfigInfo commonConfigInfo = new CommonConfigInfo();
        commonConfigInfo.setLogLevelSet(LogLevelSet());
        return commonConfigInfo;
    }

    public static GeneralProperties getConfig() {
        return config;
    }
}
