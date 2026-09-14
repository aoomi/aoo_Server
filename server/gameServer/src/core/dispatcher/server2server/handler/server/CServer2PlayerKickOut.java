package core.dispatcher.server2server.handler.server;

import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.common.utils.GsonUtils;
import core.dispatcher.IServerBaseHandler;
import jsproto.c2s.iclass.server.CServer_PlayerKickOut;

import java.util.Objects;

public class CServer2PlayerKickOut extends IServerBaseHandler {
    @Override
    public void handleMessage(String data) {
        CServer_PlayerKickOut playerKickOut = GsonUtils.stringToBean(data, CServer_PlayerKickOut.class);
        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(playerKickOut.getPid());
        if (Objects.nonNull(player)) {
            player.getClientSession().losePlayer();
            player.loseSession();
        }
    }
}