package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class C1004_Login extends BaseSendMsg {
    /**
     * 账号Id
     */
    public long accountID;
    /**
     * 打开Id
     */
    public String openid;
    /**
     * 唯一Id(微信Id)
     */
    public String unionid;
    /**
     * 验证值
     */
    public String token;
    /**
     * 昵称
     */
    public String nickName;
    /**
     * 性别
     */
    public byte sex;
    /**
     * 头像url
     */
    public String headImageUrl;
    /**
     * 服务Id
     */
    public int serverID;
    /**
     * 版本
     */
    public String version;
    /**
     * 1:手机登录
     */
    public int isMobile;

    /**
     * 客户端加密Token
     */
    public String clientToken;


    public static C1004_Login make(long accountID, String openid, String unionid, String token, String nickName, byte sex, String headImageUrl, int serverID, String version) {
        C1004_Login ret = new C1004_Login();
        ret.accountID = accountID;
        ret.openid = openid;
        ret.unionid = unionid;
        ret.token = token;
        ret.nickName = nickName;
        ret.sex = sex;
        ret.headImageUrl = headImageUrl;
        ret.serverID = serverID;
        ret.version = version;

        return ret;
    }
}