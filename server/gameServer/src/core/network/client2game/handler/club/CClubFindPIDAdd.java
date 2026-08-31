package core.network.client2game.handler.club;

import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define.Club_PARTNER;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.iclass.club.CClub_FindPIDAdd;

import java.io.IOException;

/**
 * 亲友圈管理页面，管理员 输入玩家ID，直接拉人进亲友圈
 *
 * @author Administrator
 */
public class CClubFindPIDAdd extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_FindPIDAdd req = new Gson().fromJson(message, CClub_FindPIDAdd.class);
        // 检查是否亲友圈管理员
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.clubId, player.getPid())) {
            request.error(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", req.clubId, player.getPid());
            return;
        }
        // 获取邀请的玩家信息
        Player findPlayer = PlayerMgr.getInstance().getPlayer(req.pid);
        if (null == findPlayer) {
            request.error(ErrorCode.Player_PidError, "Player_PidError");
            return;
        }
        SData_Result result = ClubMember.checkExistJoinOrQuitTimeLimit(findPlayer.getPid(), req.clubId, Club_Player_Status.PLAYER_JIARU.value(), true);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        if (ClubMgr.getInstance().getClubMemberMgr().onInsertClubMember(findPlayer, req.clubId, Club_PARTNER.Club_PARTNER_NULL.value(), 0, Club_Player_Status.PLAYER_JIARU.value(), player.getPid(), 0, 0L)) {
            request.response();
        } else {
            request.error(ErrorCode.NotAllow, "CUnionFindClubSignAdd onInsertClubMember  clubId = " + req.clubId);
        }

    }

}
