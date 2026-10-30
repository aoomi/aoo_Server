package jsproto.c2s.iclass.server;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CServer_ServerOnline extends BaseSendMsg {
    /**
     * 订阅者
     */
    private String subscribe;

    /**
     * 服务端名字
     */
    private String serverName;

    public static CServer_ServerOnline make(String subscribe, String serverName) {
        CServer_ServerOnline ret = new CServer_ServerOnline();
        ret.subscribe = subscribe;
        ret.serverName = serverName;
        return ret;
    }
}
