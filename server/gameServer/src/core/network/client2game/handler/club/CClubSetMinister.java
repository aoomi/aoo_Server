package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareunion.ShareUnionMemberMgr;
import business.global.union.UnionMember;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.common.Config;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.Club_define.Club_MINISTER;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.club.CClub_SetMinister;

import java.io.IOException;
import java.util.Objects;

/**
 * 改变俱乐部玩家状态
 *
 * @author zaf
 */
public class CClubSetMinister extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_SetMinister req = new Gson().fromJson(message, CClub_SetMinister.class);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "CClubPartnerFindInfo CLUB_NOT_EXIST ClubID:{%d}", req.clubId);
            return;
        }

        if (club.getClubListBO().getOwnerID() != player.getPid()) {
            request.error(ErrorCode.NotAllow, "CClubSetMinister you is not creater minister OwnerID:{%d}", club.getClubListBO().getOwnerID());
            return;
        }

        if (player.getPid() == req.pid) {
            request.error(ErrorCode.NotAllow, "CClubSetMinister player.getPid() == req.pid OwnerID:{%d}", club.getClubListBO().getOwnerID());
            return;
        }

        ClubMember otherMember = ClubMgr.getInstance().getClubMemberMgr().find(req.pid, req.clubId, Club_Player_Status.PLAYER_JIARU);
        if (null == otherMember) {
            request.error(ErrorCode.NotAllow, "CClubSetMinister not find otherMember other pid =" + req.pid);
            return;
        }

        Player toPlayer = PlayerMgr.getInstance().getPlayer(req.pid);
        if (null == toPlayer) {
            request.error(ErrorCode.Player_PidError, "null == toPlayer Pid:{%d}", req.pid);
            return;
        }

        if (Club_MINISTER.Club_MINISTER_CREATER.value() == req.minister) {
            request.error(ErrorCode.NotAllow, "CClubSetMinister not set minister create ");
            return;
        }

        // 检查是否管理员上限
        if (Club_MINISTER.Club_MINISTER_MGR.value() == req.minister && ClubMgr.getInstance().getClubMemberMgr().checkClubMinisterUpperLimit(club.getClubListBO().getId())) {
            request.error(ErrorCode.CLUB_SETMINISTERMAXTATNTWO, "CClubSetMinister  minister count max than two ");
            return;
        }
        //如果是设置赛事管理员的话  只有盟主的圈主才能设置
        if (req.minister == Club_MINISTER.Club_MINISTER_UNIONMGR.value()) {
            UnionMember unionMember;
            if (Config.isShare()) {
                unionMember = ShareUnionMemberMgr.getInstance().getAllOneClubUnionMember(req.clubId).values().stream().filter(k -> k.getUnionMemberBO().getClubId() == req.clubId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny().orElse(null);
            } else {
                unionMember = UnionMgr.getInstance().getUnionMemberMgr().getUnionMemberMap().values().stream().filter(k -> k.getUnionMemberBO().getClubId() == req.clubId && k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())).findAny().orElse(null);
            }
            //如果找不到赛事成员 说明亲友圈还没有加入赛事 这时候不存在重复拉人
            if (Objects.isNull(unionMember)) {
                request.error(ErrorCode.NotAllow, "CClubSetMinister not set minister union create ");
                return;
            }
            if (!unionMember.isCreate()) {
                request.error(ErrorCode.NotAllow, "CClubSetMinister not set minister union create ");
                return;
            }
        }
        request.response();
        otherMember.setIsminister(toPlayer, club, req.minister, player.getPid());
    }

}
