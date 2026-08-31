package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class C1001_CreateRole extends BaseSendMsg {

    public String nickName;
    public byte sex;
    public String headImageUrl;
    public long accountID;
    public int isMobile;
    public long Phone;


    public static C1001_CreateRole make(String nickName, byte sex, String headImageUrl, long accountID, int isMobile) {
        C1001_CreateRole ret = new C1001_CreateRole();
        ret.nickName = nickName;
        ret.sex = sex;
        ret.headImageUrl = headImageUrl;
        ret.accountID = accountID;
        ret.isMobile = isMobile;

        return ret;


    }
}