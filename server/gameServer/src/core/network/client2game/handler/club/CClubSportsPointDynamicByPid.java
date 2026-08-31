package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.cclass.union.UnionDynamicItem;
import jsproto.c2s.iclass.club.CClub_Dynamic;
import jsproto.c2s.iclass.club.CClub_RoomPromotionPoint;
import jsproto.c2s.iclass.club.SClub_UnionDynamic;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 亲友圈竞技点动态
 *
 * @author zaf
 */
public class CClubSportsPointDynamicByPid extends PlayerHandler {

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
        //报名费新的话 单独走
        if (req.getChooseType() == UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.ENTRYFEE_NEW.value()) {
            SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getRoomPromotionPointList(new CClub_RoomPromotionPoint(req.getGetType(), req.getPid() == 0 ? player.getPid() : req.getPid()));
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
            return;
        }
        List<UnionDynamicItem> unionDynamicItemList = UnionMgr.getInstance().unionDynamicByPid(player.getPid(), req.getClubId(), club.getClubListBO().getUnionId(), req.getPageNum(), req.getGetType(), req.getChooseType(), player.getPid());
        if (req.getPageNum() <= 1) {
            request.response(unionDynamicItemList);
        } else {
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayerByOnline(player.getPid());
            if (Objects.nonNull(sharePlayer)) {
                player.pushProtoMq(SClub_UnionDynamic.make(unionDynamicItemList, req.getPageNum()));
            }
        }
    }

}
