package core.network.client2game.handler.union;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.Club_define.Club_Player_Status;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.union.CUnion_FindPIDAdd;

import java.io.IOException;

/**
 * 赛事管理页面，管理员 输入玩家ID，直接拉人进赛事
 *
 * @author Administrator
 */
public class CUnionFindClubSignAdd extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CUnion_FindPIDAdd req = new Gson().fromJson(message, CUnion_FindPIDAdd.class);
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotManage(player.getPid(), req.getClubId(), req.getUnionId())) {
            // 	此按钮仅盟主、联盟管理可用；
            request.error(ErrorCode.UNION_NOT_MANAGE, "ClubSignAdd UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", player.getPid(), req.getClubId(), req.getUnionId());
            return;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubSign());
        if (null == club) {
            // 亲友圈不存在
            request.error(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            return;
        }
        if (club.getClubListBO().getUnionId() > 0L) {
            // 该亲友圈已在其他联盟
            request.error(ErrorCode.UNION_EXIST_ADD_OTHERS_UNION, "UNION_EXIST_ADD_OTHERS_UNION");
            return;
        }

        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().find(club.getOwnerPlayer().getPid(), club.getClubListBO().getId(), Club_Player_Status.PLAYER_JIARU);
        if (null == clubMember) {
            request.error(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER");
            return;
        }

        if (UnionMgr.getInstance().getUnionMemberMgr().onInsertUnionMember(club.getOwnerPlayer(), req.getUnionId(), club.getClubListBO().getId(), UnionDefine.UNION_PLAYER_STATUS.PLAYER_YAOQING.value(), player.getPid(), clubMember.getId())) {
            request.response();
        } else {
            request.error(ErrorCode.UNION_PLAYER_YAOQING_FAIL, "UNION_PLAYER_YAOQING_FAIL");
        }
    }

}
