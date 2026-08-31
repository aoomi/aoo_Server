package cenum;

/**
 * 公共字段枚举
 */
public enum CommonFieldEnum {
    /**
     * 玩家id
     */
    ID("id", "id"),

    /**
     * 头部
     */
    HEAD("Head","Head"),

    /**
     * 微信appId
     */
    WX_APPID("appid","appid"),
    /**
     * 秘钥
     */
    WX_SECRET("secret","secret"),
    /**
     * 验证码
     */
    WX_CODE("code","code"),


    /**
     * 电话id
     */
    MOBILE_ID("mobile","mobile"),
    /**
     * 电话验证码
     */
    MOBILE_CODE("code","code"),

    /**
     * 授权类型
     */
    GRANT_TYPE("grant_type","grant_type"),
    /**
     * 访问令牌
     */
    ACCESS_TOKEN("access_token","access_token"),
    /**
     * 开放id
     */
    OPENID("openid","openid"),
    /**
     * 唯一id
     */
    UNIONID("unionid","unionid"),
    /**
     * 语言
     */
    LANG("lang","lang"),
    /**
     * 错误码
     */
    ERRCODE("errcode","errcode"),
    /**
     * 性别
     */
    SEX("sex","sex"),
    /**
     * 昵称
     */
    NICK_NAME("nickname","nickname"),
    /**
     * 头像
     */
    HEAD_IMG_URL("headimgurl","headimgurl"),

    /**
     * 结束时间
     */
    END_TICK("endTick","endTick"),

    /**
     * 刷新token
     */
    REFRESH_TOKEN("refresh_token","refresh_token"),

    /**
     * 到期......到
     */
    EXPIRES_IN("expires_in","expires_in")
    ;

    private String value;
    private String field;

    CommonFieldEnum(String value, String field) {
        this.value = value;
        this.field = field;
    }

    public String value() {
        return value;
    }

    public String field() {
        return field;
    }
}
