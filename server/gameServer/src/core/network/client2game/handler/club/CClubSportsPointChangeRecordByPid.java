package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.iclass.club.CClub_Dynamic;

import java.io.IOException;
import java.util.Objects;

/**
 * 显示该玩家的个人积分变化记录
 * 通过竞技动态的形式去显示
 *
 * @author zaf
 */
public class CClubSportsPointChangeRecordByPid extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_Dynamic req = new Gson().fromJson(message, CClub_Dynamic.class);
        boolean checkClubMemberStateExist = ClubMgr.getInstance().getClubMemberMgr().checkClubMemberStateExist(req.getClubId(), player.getPid(), Club_define.Club_Player_Status.PLAYER_JIARU.value());
        if (!checkClubMemberStateExist) {
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CClubSportsPointDynamicByPid you have not minister");
            return;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            return;
        }
        if (club.getClubListBO().getUnionId() <= 0L) {
            request.error(ErrorCode.CLUB_NOT_JOIN_UNION, "CLUB_NOT_JOIN_UNION");
            return;
        }
        SData_Result result = UnionMgr.getInstance().getCompetitionRecord(player, req.getClubId(), club.getClubListBO().getUnionId(), req.getPageNum(), req.getGetType(), req.getChooseType(), player.getPid(), req.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }

    }

}
