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
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.iclass.club.CClub_SetMinister;

import java.io.IOException;

/**
 * 改变俱乐部玩家状态
 *
 * @author zaf
 */
public class CClubSetPromotionMinister extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_SetMinister req = new Gson().fromJson(message, CClub_SetMinister.class);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "CClubPartnerFindInfo CLUB_NOT_EXIST ClubID:{%d}", req.clubId);
            return;
        }
        if (player.getPid() == req.pid) {
            request.error(ErrorCode.NotAllow, "CClubSetMinister player.getPid() == req.pid OwnerID:{%d}", club.getClubListBO().getOwnerID());
            return;
        }

        ClubMember selfMember = ClubMgr.getInstance().getClubMemberMgr().find(player.getPid(), req.clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == selfMember) {
            request.error(ErrorCode.NotAllow, "CClubSetMinister not find otherMember other pid =" + req.pid);
            return;
        }
        if (selfMember.isNotLevelPromotion()) {
            request.error(ErrorCode.CLUB_NOT_PROMOTION, "CClubSetMinister not find otherMember other pid =" + req.pid);
            return;
        }
        ClubMember otherMember = ClubMgr.getInstance().getClubMemberMgr().find(req.pid, req.clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == otherMember) {
            request.error(ErrorCode.NotAllow, "CClubSetMinister not find otherMember other pid =" + req.pid);
            return;
        }
//		if(otherMember.isLevelPromotion()){
//			request.error(ErrorCode.CLUB_EXIST_PROMOTION, "CClubSetMinister CLUB_EXIST_PROMOTION "+req.pid);
//			return;
//		}
        //判断是不是直属的下线
        if (selfMember.getId() != otherMember.getClubMemberBO().getUpLevelId()) {
            request.error(ErrorCode.CLUB_NOT_SUBORDINATE, "CClubSetMinister CLUB_NOT_SUBORDINATE " + req.pid);
            return;
        }
        Player toPlayer = PlayerMgr.getInstance().getPlayer(req.pid);
        if (null == toPlayer) {
            request.error(ErrorCode.Player_PidError, "null == toPlayer Pid:{%d}", req.pid);
            return;
        }
        // 检查是否管理员上限
        if (ClubMgr.getInstance().getClubMemberMgr().checkPromotionMinisterUpperLimit(club.getClubListBO().getId(), selfMember)) {
            request.error(ErrorCode.CLUB_SETMINISTERMAXTATNTWO, "CClubSetMinister  minister count max than two ");
            return;
        }
        request.response();
        otherMember.setPromotionMinister(toPlayer, club, req.minister, player.getPid());
    }

}
