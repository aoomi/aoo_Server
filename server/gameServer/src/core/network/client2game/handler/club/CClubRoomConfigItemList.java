package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.club.CClub_BanGame;

import java.io.IOException;
import java.util.Objects;

public class CClubRoomConfigItemList extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_BanGame req = new Gson().fromJson(message, CClub_BanGame.class);
        // 获取亲友圈信息。
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.NotAllow, "null == club clubId:{}", req.clubId);
            return;
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, player.getPid());
        if (Objects.isNull(clubMember)) {
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER clubId:{}", req.clubId);
            return;
        }
        request.response(club.getClubRoomConfigItemList(clubMember.getClubMemberBO().getConfigId()));
    }
}
