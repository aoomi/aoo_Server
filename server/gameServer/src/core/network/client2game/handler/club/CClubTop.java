package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.iclass.club.CClub_Top;

import java.io.IOException;

/**
 * 亲友圈置顶设置
 *
 * @author zaf
 */
public class CClubTop extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_Top req = new Gson().fromJson(message, CClub_Top.class);
        // 获取被操作玩家的信息
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            request.error(ErrorCode.NotAllow, "null == clubMember not Pid:{%d}", player.getPid());
            return;
        }
        // 获取亲友圈信息。
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.NotAllow, "null == club clubId:{%d}", req.clubId);
            return;
        }
        // 设置置顶时间
        clubMember.getClubMemberBO().saveTopTime(CommTime.nowSecond());
        SData_Result result = ClubMgr.getInstance().onGetClubListByPlayer(player);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
