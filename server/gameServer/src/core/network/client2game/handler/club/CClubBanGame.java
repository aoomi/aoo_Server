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
import jsproto.c2s.cclass.club.Club_define.Club_MINISTER;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.iclass.club.CClub_BanGame;

import java.io.IOException;

/**
 * 亲友圈禁止游戏
 *
 * @author zaf
 */
public class CClubBanGame extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_BanGame req = new Gson().fromJson(message, CClub_BanGame.class);
        // 检查当前操作者是否管理员、创建者。
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.clubId, player.getPid())) {
            request.error(ErrorCode.NotAllow, "not club admin ClubID:{}", req.clubId);
            return;
        }
        //
        Player banPlayer = PlayerMgr.getInstance().getPlayer(req.banPid);
        if (null == banPlayer) {
            request.error(ErrorCode.Player_PidError, "error banPid:{%d}", req.banPid);
            return;
        }
        // 获取被操作玩家的信息
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(req.banPid, req.clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            request.error(ErrorCode.NotAllow, "null == clubMember not banPid:{}", req.banPid);
            return;
        }
        // 检查被操作的玩家是创建者
        if (Club_MINISTER.Club_MINISTER_CREATER.value() == clubMember.getClubMemberBO().getIsminister()) {
            request.error(ErrorCode.NotAllow, "create club not banPid:{}", req.banPid);
            return;
        }
        // 获取亲友圈信息。
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.NotAllow, "null == club clubId:{}", req.clubId);
            return;
        }
        clubMember.getClubMemberBO().saveBanGame(req.isBan ? 1 : 0);
        request.response();
        Club_Player_Status status = req.isBan ? Club_Player_Status.PLAYER_BECOME_BAN : Club_Player_Status.PLAYER_CANCEL_BAN;
        ClubMgr.getInstance().getClubMemberMgr().notify2AllClubMemberAndPid(banPlayer, status.value(), club, clubMember);
    }
}
