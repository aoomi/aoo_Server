package core.network.client2game.handler.club;

import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.club.CClub_Dynamic;

import java.io.IOException;

/**
 * 亲友圈动态
 *
 * @author zaf
 */
public class CClubDynamic extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_Dynamic req = new Gson().fromJson(message, CClub_Dynamic.class);
        boolean isMinister = ClubMgr.getInstance().getClubMemberMgr().isMinister(req.getClubId(), player.getPid());
        if (!isMinister) {
            request.error(ErrorCode.CLUB_NOTMINISTER, "CClubDynamic you have not minister");
            return;
        }
        request.response(UnionMgr.getInstance().clubDynamic(req.getClubId(), req.getPageNum(), req.getGetType(), req.getPid(), req.getExecPid(), req.getUnionId(), player.getPid()));
    }

}
