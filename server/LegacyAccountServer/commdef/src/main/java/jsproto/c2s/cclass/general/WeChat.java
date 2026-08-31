package jsproto.c2s.cclass.general;

import lombok.Data;

@Data
public class WeChat  {
    /**
     * AppId
     */
    private String appid;
    /**
     * 秘钥
     */
    private String secret;
    /**
     * 授权类型
     */
    private String grantType;
    /**
     * 语言类型
     */
    private String langType;
    /**
     * 个人信息授权url
     */
    private String accountUserInfoUrl;
    /**
     * 刷新token Url
     */
    private String refreshTokenUrl;
    /**
     * 获取access Url
     */
    private String accessTokenUrl;

    /**
     * 获取公众号玩家数据
     */
    private String gongZhonghaoUserInfoUrl;

}
