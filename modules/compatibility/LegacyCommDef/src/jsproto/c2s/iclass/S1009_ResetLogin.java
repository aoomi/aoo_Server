package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class S1009_ResetLogin extends BaseSendMsg {
    /**
     * 时间戳
     */
    public long time;
    /**
     * 时区
     */
    public int timeZone;
    /**
     * 开服时间
     */
    public long startServerTime;
    /**
     * 默认公会ID
     */
    public long defaultFamilyID;
    /**
     * 是否创建角色(0:存在角色可直接登录,1:需要创建角色请求[base.C1001CreateRole])
     */
    public byte isNeedCreateRole;
    /**
     * 账号Id
     */
    public long accountID;
    /**
     * 创建的角色是否是手机登录
     */
    public int isMobile;

    /**
     * 服务端随机Token
     */
    public String serverToken;

    public static S1009_ResetLogin make(long time, int timeZone, long startServerTime, long defaultFamilyID, byte isNeedCreateRole, long accountID, int isMobile, String serverToken) {
        S1009_ResetLogin ret = new S1009_ResetLogin();
        ret.time = time;
        ret.timeZone = timeZone;
        ret.startServerTime = startServerTime;
        ret.defaultFamilyID = defaultFamilyID;
        ret.isNeedCreateRole = isNeedCreateRole;
        ret.accountID = accountID;
        ret.isMobile = isMobile;
        ret.serverToken = serverToken;

        return ret;


    }
}