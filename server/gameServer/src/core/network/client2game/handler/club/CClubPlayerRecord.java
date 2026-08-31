package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_GetRecord;

import java.io.IOException;

/**
 * 亲友圈普通成员战绩界面优化
 */
public class CClubPlayerRecord extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_GetRecord req = new Gson().fromJson(message, CClub_GetRecord.class);
        SData_Result result = ClubMgr.getInstance().getClubRankMgr().getClubPlayerRecord(req, player.getPid());
        request.response(result.getData());
    }
}
