package jsproto.c2s.cclass.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfo implements Serializable {
    private String uid;
    private int sex;
    private String nickName;
    private String headImageUrl;
    private String token;
    private String openid;
    private String unionid;
    private String charAccountPsw;

    public UserInfo(String uid, int sex, String nickName, String headImageUrl, String token, String openid, String unionid,String charAccountPsw) {
        this.uid = uid;
        this.sex = sex;
        this.nickName = nickName;
        this.headImageUrl = headImageUrl;
        this.token = token;
        this.openid = openid;
        this.unionid = unionid;
        this.charAccountPsw = charAccountPsw;
    }
    public static final UserInfo Of(String uid, int sex, String nickName, String headImageUrl, String token, String openid, String unionid,String charAccountPsw) {
        return new UserInfo(uid,sex,nickName,headImageUrl,token,openid,unionid,charAccountPsw);
    }
}
