package cenum.sdk;

public enum  SDKTypeEnum {
    /**
     * 公司自己
     */
    SDKType_Company(0),
    /**
     * 微信公众号授权
     */
    SDKType_WeChat(2),
    /**
     *app授权
     */
    SDKType_WeChatApp(3),
    /**
     * 手机短信登录
     */
    SDKType_Mobile(4),
    /***
     * line 授权登录
     */
    SDKType_LineApp(5),
    /**
     * FaceBook 授权登录
     */
    SDKType_FaceBookApp(6),
    /**
     * Google 授权登录
     */
    SDKType_GoogleApp(7),
    ;

    private int value;

    SDKTypeEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
