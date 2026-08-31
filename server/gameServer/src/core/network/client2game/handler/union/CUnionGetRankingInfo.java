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
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.iclass.union.CUnion_Base;

import java.io.IOException;
import java.util.Objects;

/**
 * 赛事玩家已收到排名的通知信息
 *
 * @author Huaxing
 */
public class CUnionGetRankingInfo extends PlayerHandler {
    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CUnion_Base req = new Gson().fromJson(message, CUnion_Base.class);
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
        Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(req.getUnionId());
        if (Objects.isNull(union)) {
            // 赛事不存在
            request.error(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST");
            return;
        }
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.getClubId(), Club_define.Club_Player_Status.PLAYER_JIARU);
        if (Objects.isNull(clubMember)) {
            // 不是亲友圈成员
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
            return;
        }
        request.response();
        // 更新回合id
        clubMember.getClubMemberBO().saveRoundId(union.getUnionBO().getRoundId());

    }
}
