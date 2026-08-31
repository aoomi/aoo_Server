package jsproto.c2s.iclass.client;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class S0000_AccountLogin extends BaseSendMsg {
    /**
     * 账号id
     */
    private long AccountID;
    /**
     * 账号
     */
    private String CharAccount;
    /**
     * 账号类型
     */
    private int AccountType;
    /**
     * 头像
     */
    private String HeadImageUrl;
    /**
     * 令牌
     */
    private String Token;
    /**
     * 昵称
     */
    private String NickName;
    /**
     * 性别
     */
    private int Sex;
    /**
     * SDK令牌
     */
    private String SDKToken;
    /**
     * 打开id
     */
    private String Openid;
    /**
     * 唯一id
     */
    private String Unionid;
    /**
     * 公钥
     */
    private String PublicKey;



    public static S0000_AccountLogin make(long accountID, String charAccount, int accountType, String headImageUrl, String token, String nickName, int sex, String SDKToken, String openid, String unionid,String PublicKey) {
        S0000_AccountLogin accountLogin = new S0000_AccountLogin();
        accountLogin.setHead(0x0000);
        accountLogin.setAccountID(accountID);
        accountLogin.setCharAccount(charAccount);
        accountLogin.setAccountType(accountType);
        accountLogin.setHeadImageUrl(headImageUrl);
        accountLogin.setToken(token);
        accountLogin.setNickName(nickName);
        accountLogin.setSex(sex);
        accountLogin.setSDKToken(SDKToken);
        accountLogin.setOpenid(openid);
        accountLogin.setUnionid(unionid);
        accountLogin.setPublicKey(PublicKey);
        return accountLogin;
    }


}
