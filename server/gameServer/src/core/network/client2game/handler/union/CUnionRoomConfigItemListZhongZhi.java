package core.network.client2game.handler.union;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.iclass.union.CUnion_Base;

import java.io.IOException;

public class CUnionRoomConfigItemListZhongZhi extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CUnion_Base req = new Gson().fromJson(message, CUnion_Base.class);
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (null == union) {
            request.error(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            return;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (null == club) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            return;
        }
        if (club.getClubListBO().getUnionId() != req.getUnionId()) {
            request.error(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR club UnionId:{%d},unionId:{%d}", club.getClubListBO().getUnionId(), req.getUnionId());
            return;
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
            return;
        }
        request.response(union.getUnionRoomConfigItemListZhongZhi(clubMember));
    }
}
