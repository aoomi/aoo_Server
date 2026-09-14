package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.union.SUnion_NotifyList;

import java.io.IOException;

/**
 * 赛事通知
 */
public class CUnionNotify extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        request.response();
        player.pushProto(SUnion_NotifyList.make(UnionMgr.getInstance().onUnionInvited(player.getPid())));
    }
}
