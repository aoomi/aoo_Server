//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package core.dispatcher.server2server.handler.registry;

import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import com.ddm.server.websocket.def.SubscribeEnum;
import core.dispatcher.IServerBaseHandler;
import jsproto.c2s.iclass.registry.CRegistry_ServerOnline;

/**
 * 新服务端上线通知
 */
public class CRegistry2ServerOnline extends IServerBaseHandler {

    @Override
    public void handleMessage(String data) {
        CRegistry_ServerOnline serverOnline = GsonUtils.stringToBean(data, CRegistry_ServerOnline.class);
        MqConsumerTopicFactory.getInstance().addConnect(SubscribeEnum.valueOf(serverOnline.getServerName()), serverOnline.getSubscribe());
    }
}
