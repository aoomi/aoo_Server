package business.rocketmq;

import business.global.sharegm.ShareNodeServerMgr;
import com.ddm.server.common.Config;
import com.ddm.server.common.redis.RedisSource;
import com.ddm.server.mq.factory.ITryConnectNotify;
import core.dispatcher.RegMaooandler;
import core.dispatcher.RegNetPack;
import jsproto.c2s.iclass.registry.CRegistry_ServerOffline;
import jsproto.c2s.iclass.registry.CRegistry_ServerOnline;
import jsproto.c2s.iclass.server.CServer_ServerOnline;

public class TryConnectNotify implements ITryConnectNotify {

    @Override
    public void registryNotify() {
        RegNetPack.getInstance().regNetPack(Config.REGISTRY_NOTIFY, RegMaooandler.CRegistry2ServerOnline, CRegistry_ServerOnline.make(Config.getLocalServerTopic(), Config.ServerIDStr().toUpperCase()));
    }

    @Override
    public void serverNotify() {
        RegNetPack.getInstance().regNetPack(Config.SERVER_NOTIFY, RegMaooandler.CServer2Server, CServer_ServerOnline.make(Config.getLocalServerTopic(), Config.ServerIDStr().toUpperCase()));
    }

    @Override
    public void stopCurConnect() {
        RegNetPack.getInstance().regNetPack(Config.REGISTRY_NOTIFY, RegMaooandler.CRegistry2ServerOffline, CRegistry_ServerOffline.make(Config.getLocalServerTopic(), Config.ServerIDStr().toUpperCase(), ShareNodeServerMgr.getInstance().checkIsMasterHall()));
        RedisSource.getSetV(Config.ServerIDStr().toUpperCase()).remove(Config.getLocalServerTopic());
    }
}
