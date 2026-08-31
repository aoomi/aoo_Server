package jsproto.c2s.iclass.registry;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CRegistry_ServerOnline extends BaseSendMsg {
    /**
     * 订阅者
     */
    private String subscribe;

    /**
     * 服务端名字
     */
    private String serverName;

    public static CRegistry_ServerOnline make(String subscribe, String serverName) {
        CRegistry_ServerOnline ret = new CRegistry_ServerOnline();
        ret.subscribe = subscribe;
        ret.serverName = serverName;
        return ret;
    }
}
