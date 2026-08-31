package core.network.client2game.handler.union;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.iclass.union.CUnion_Apply;

import java.io.IOException;
import java.util.Objects;

/**
 * 赛事申请操作
 * 我要退赛
 * 取消退赛
 * 申请复赛
 *
 * @author Huaxing
 */
public class CUnionApply extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CUnion_Apply req = new Gson().fromJson(message, CUnion_Apply.class);
        if (player.getRoomInfo().getRoomId() > 0L) {
            request.error(ErrorCode.PLAYER_IN_GAME_ERROR, "PLAYER_IN_GAME_ERROR");
            return;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (Objects.isNull(club)) {
            // 亲友圈不存在
            request.error(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            return;
        }
        if (club.getClubListBO().getUnionId() != req.getUnionId()) {
            // 不是这个赛事
            request.error(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
            return;
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember)) {
            // 不是亲友圈成员
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
            return;
        }
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            request.error(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            return;
        }
        //中至的亲友圈 圈主也可以进行退赛
        if (union.getUnionBO().getOwnerId() == player.getPid() && !union.isZhongZhiUnion()) {
            request.error(ErrorCode.NotAllow, "UNION_CREATE");
            return;
        }
        SData_Result result = clubMember.execApply(req.getUnionId(), union.getUnionBO().getOutSports());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getCustom());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
