package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.iclass.club.CClub_FindPIDAdd;
import jsproto.c2s.iclass.club.SClub_FindPIDInfo;

import java.io.IOException;

/**
 * 亲友圈-查询
 *
 * @author zaf
 */
public class CClubFindPIDInfo extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_FindPIDAdd req = new Gson().fromJson(message, CClub_FindPIDAdd.class);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "CUnionFindClubSignInfo CLUB_NOT_EXIST ClubID:{%d}", req.clubId);
            return;
        }
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.clubId, player.getPid())) {
            request.error(ErrorCode.CLUB_NOTMINISTER, "CUnionFindClubSignInfo not Minister ClubID:{%d}", req.clubId);
            return;
        }

        Player toPlayer = PlayerMgr.getInstance().getPlayer(req.pid);
        if (null == toPlayer) {
            // 玩家不存在
            request.error(ErrorCode.Player_PidError, "CUnionFindClubSignInfo null == toPlayer ClubID:{%d},Pid:{%d}", req.clubId, req.pid);
            return;
        }
        int state = Club_Player_Status.PLAYER_NOMARL.value();
        if (ClubMgr.getInstance().getClubMemberMgr().checkClubMemberStateExist(req.clubId, req.pid, Club_Player_Status.PLAYER_JIARU.value())) {
            state = Club_Player_Status.PLAYER_JIARU.value();
        }
        request.response(SClub_FindPIDInfo.make(toPlayer.getShortPlayer(), state));
    }

}
