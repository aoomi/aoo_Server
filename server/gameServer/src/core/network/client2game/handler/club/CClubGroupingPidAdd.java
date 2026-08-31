package core.network.client2game.handler.club;

import business.global.club.Club;
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
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.iclass.club.CClub_GroupingPid;
import jsproto.c2s.iclass.club.SClub_GroupingInfo;

import java.io.IOException;
import java.util.Objects;

/**
 * 亲友圈分组增加
 *
 * @author zaf
 */
public class CClubGroupingPidAdd extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_GroupingPid req = new Gson().fromJson(message, CClub_GroupingPid.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().checkClubGetMemberPromotionListAdd(req.clubId, player.getPid(), req.pid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        // 检查成员是否在本亲友圈。
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(req.pid, req.clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "null == clubMember ClubID:{%d}", req.clubId);
            return;
        }
        // 获取指定的亲友圈信息
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "null == club ClubID:{%d}", req.clubId);
            return;
        }

        Player opPlayer = PlayerMgr.getInstance().getPlayer(req.pid);
        if (Objects.isNull(opPlayer)) {
            request.error(ErrorCode.Player_PidError, "CClubGroupingPidAdd null == opPlayer ClubID:{%d}", req.clubId);
            return;
        }

        result = club.addClubGroupingPid(req.groupingId, req.pid);
        // 将玩家添加到指定组中。
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }
        request.response(SClub_GroupingInfo.make(req.clubId, req.groupingId, opPlayer.getShortPlayer()));
    }
}
