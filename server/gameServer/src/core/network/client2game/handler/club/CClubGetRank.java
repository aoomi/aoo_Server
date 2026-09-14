package core.network.client2game.handler.club;

import business.global.club.ClubListMgr;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.iclass.club.CClub_GetRank;

import java.io.IOException;

/**
 * 加入俱乐部
 *
 * @author zaf
 */
public class CClubGetRank extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {

        final CClub_GetRank req = new Gson().fromJson(message, CClub_GetRank.class);

        ClubMember myClubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == myClubMember) {
            request.error(ErrorCode.NotAllow, "CClubGetRank not find myClubMember");
            return;
        }

        if (!myClubMember.isMinister()) {
            request.error(ErrorCode.CLUB_NOTMINISTER, "CClubGetRank you have not minister");
            return;
        }

        ClubListMgr listMgr = ClubMgr.getInstance().getClubListMgr();
//    	listMgr.getRank(request, req);
    }

}
