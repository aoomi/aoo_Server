package jsproto.c2s.cclass.general;

import lombok.Data;

/**
 * 公共配置
 */
@Data
public class GeneralInfo {
    /**
     * 服务端名称
     */
    private String ServerName;
    /**
     * 微信管理服
     */
    private String WeChatServerUrl;
    /**
     * 账号管理服
     */
    private String AccountServerUrl;
    /**
     * 资源管理服
     */
    private String ResServerUrl;
    /**
     * 订单管理服
     */
    private String OrderServerUrl;
    /**
     * 管理服
     */
    private String ManagerServerUrl;
}

