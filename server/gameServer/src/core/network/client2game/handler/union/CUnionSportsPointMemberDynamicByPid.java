package core.network.client2game.handler.union;

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
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.cclass.union.UnionDynamicItem;
import jsproto.c2s.iclass.club.CClub_RoomPromotionPoint;
import jsproto.c2s.iclass.club.SClub_UnionDynamic;
import jsproto.c2s.iclass.union.CUnion_SportsPointMemberDynamicByPid;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 赛事指定成员竞技点动态
 *
 * @author zaf
 */
public class CUnionSportsPointMemberDynamicByPid extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CUnion_SportsPointMemberDynamicByPid req = new Gson().fromJson(message, CUnion_SportsPointMemberDynamicByPid.class);
        if (UnionMgr.getInstance().getUnionMemberMgr().isNotUnionManage(player.getPid(), req.getClubId(), req.getUnionId())) {
            request.error(ErrorCode.UNION_NOT_MANAGE, "UNION_NOT_MANAGE pid:{%d},clubId:{%d},unionId:{%d}", player.getPid(), req.getClubId(), req.getUnionId());
            return;
        }
        //报名费新的话 单独走
        if (req.getChooseType() == UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.ENTRYFEE_NEW.value()) {
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
            if (Objects.isNull(club)) {
                request.error(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
                return;
            }
            if (club.getClubListBO().getUnionId() != req.getUnionId()) {
                request.error(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
                return;
            }
            SData_Result result = ClubMgr.getInstance().getClubMemberMgr().getRoomPromotionPointList(new CClub_RoomPromotionPoint(req.getGetType(), req.getOpPid() == 0 ? player.getPid() : req.getOpPid()));
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
            return;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getOpClubId());
        if (Objects.isNull(club)) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST");
            return;
        }
        if (club.getClubListBO().getUnionId() != req.getUnionId()) {
            request.error(ErrorCode.UNION_ID_ERROR, "UNION_ID_ERROR");
            return;
        }

        List<UnionDynamicItem> unionDynamicItemList = UnionMgr.getInstance().unionDynamicByPid(req.getOpPid(), req.getOpClubId(), req.getUnionId(), req.getPageNum(), req.getGetType(), req.getChooseType(), player.getPid());
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
