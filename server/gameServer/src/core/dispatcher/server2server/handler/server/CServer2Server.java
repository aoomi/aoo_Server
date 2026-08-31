package core.dispatcher.server2server.handler.server;


import com.ddm.server.common.utils.GsonUtils;
import core.dispatcher.IServerBaseHandler;
import jsproto.c2s.iclass.registry.CRegistry_ServerOnline;


/**
 * 服务端通知
 */
public class CServer2Server extends IServerBaseHandler {

    @Override
    public void handleMessage(String data) {
        CRegistry_ServerOnline serverOnline = GsonUtils.stringToBean(data, CRegistry_ServerOnline.class);
    }
}

