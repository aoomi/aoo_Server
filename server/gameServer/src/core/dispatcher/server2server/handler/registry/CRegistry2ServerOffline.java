package core.dispatcher.server2server.handler.registry;

import com.ddm.server.common.utils.GsonUtils;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import com.ddm.server.websocket.def.SubscribeEnum;
import core.dispatcher.IServerBaseHandler;
import jsproto.c2s.iclass.registry.CRegistry_ServerOffline;

/**
 * 新服务端下线通知
 */
public class CRegistry2ServerOffline extends IServerBaseHandler {

    @Override
    public void handleMessage(String data) {
        CRegistry_ServerOffline serverOffline = GsonUtils.stringToBean(data, CRegistry_ServerOffline.class);
        MqConsumerTopicFactory.getInstance().removeConnect(SubscribeEnum.valueOf(serverOffline.getServerName()), serverOffline.getSubscribe());
    }
}
