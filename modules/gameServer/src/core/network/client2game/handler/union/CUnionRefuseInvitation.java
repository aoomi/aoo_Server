package core.network.client2game.handler.union;

import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.union.CUnion_Join;

import java.io.IOException;

/**
 * 赛事拒绝邀请
 */
public class CUnionRefuseInvitation extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CUnion_Join req = new Gson().fromJson(message, CUnion_Join.class);
        request.response();
        // 拒绝邀请不返回成功失败。
        UnionMgr.getInstance().getUnionListMgr().onUnionRefuseInvitation(player, req);

    }
}
