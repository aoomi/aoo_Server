package jsproto.c2s.iclass.registry;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CRegistry_ServerOffline extends BaseSendMsg {
    /**
     * 订阅者
     */
    private String subscribe;

    /**
     * 服务端名字
     */
    private String serverName;

    /**
     * 是否强制退出
     */
    private boolean force;

    public static CRegistry_ServerOffline make(String subscribe, String serverName, boolean force) {
        CRegistry_ServerOffline ret = new CRegistry_ServerOffline();
        ret.subscribe = subscribe;
        ret.serverName = serverName;
        ret.force = force;
        return ret;
    }
}
