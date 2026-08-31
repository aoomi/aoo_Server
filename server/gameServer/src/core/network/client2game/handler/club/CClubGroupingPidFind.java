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
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.iclass.club.CClub_GroupingPid;

import java.io.IOException;
import java.util.Objects;

/**
 * 赛事成员查询
 *
 * @author zaf
 */
public class CClubGroupingPidFind extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_GroupingPid req = new Gson().fromJson(message, CClub_GroupingPid.class);
        // 检查当前操作者是否管理员、创建者。
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.clubId, player.getPid())) {
            ClubMember doclubMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
            if (null == doclubMember) {
                request.error(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "null == doclubMember ClubID:{%d}", req.clubId);
                return;
            }
            if (doclubMember.isLevelPromotion() || doclubMember.isPromotionManage()) {
            } else {
                request.error(ErrorCode.NotAllow, "not club admin ClubID:{%d}", req.clubId);
                return;
            }

        }
        // 检查成员是否在本亲友圈。
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(req.pid, req.clubId, Club_define.Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            request.error(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "null == clubMember ClubID:{%d}", req.clubId);
            return;
        }

        Player toPlayer = PlayerMgr.getInstance().getPlayer(req.pid);
        if (Objects.isNull(toPlayer)) {
            request.error(ErrorCode.Player_PidError, "CClubGroupingPidFind toPlayer ClubID:{%d}", req.clubId);
            return;
        }
        request.response(toPlayer.getShortPlayer());
    }
}
