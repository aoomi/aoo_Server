package core.network.client2game.handler.union;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.union.CUnion_FindPIDAdd;
import jsproto.c2s.iclass.union.SUnion_FindClubSignInfo;

import java.io.IOException;

/**
 * 赛事-查询
 *
 * @author zaf
 */
public class CUnionFindClubSignInfo extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_FindPIDAdd req = new Gson().fromJson(message, CUnion_FindPIDAdd.class);
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotManage(player.getPid(), req.getClubId(), req.getUnionId())) {
            // 	此按钮仅盟主、联盟管理可用；
            request.error(ErrorCode.UNION_NOT_MANAGE, "ClubSignInfo UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", player.getPid(), req.getClubId(), req.getUnionId());
            return;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubSign());
        if (null == club) {
            // 亲友圈不存在
            request.error(ErrorCode.CLUB_NOT_EXIST, "ClubSignInfo CLUB_NOT_EXIST");
            return;
        }
        request.response(SUnion_FindClubSignInfo.make(club.getClubListBO().getName(), club.getOwnerPlayer().getName()));
    }

}
