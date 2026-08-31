package core.network.client2game.handler.club;

import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.player.Player;
import com.ddm.server.common.Config;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_PartnerPersonalRecord;

import java.io.IOException;
import java.util.Objects;

/**
 * 亲友圈合伙人个人战绩统计
 * 创建者、管理员、指定合伙人可以查询。
 *
 * @author Administrator
 */
public class CClubPromotionPersonalCount extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_PartnerPersonalRecord req = new Gson().fromJson(message, CClub_PartnerPersonalRecord.class);
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, player.getPid());
        if (clubMember.isNotClubCreate() && clubMember.isNotPromotion() && clubMember.isNotLevelPromotion()) {
            if (!clubMember.isPromotionManage()) {
                request.error(ErrorCode.CLUB_NOT_PROMOTION, "CClubPromotionPersonalCount CLUB_NOT_PROMOTION");
                return;
            }
            if (Config.isShare()) {
                clubMember = ShareClubMemberMgr.getInstance().getClubMember(clubMember.getClubMemberBO().getUpLevelId());
            } else {
                clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(clubMember.getClubMemberBO().getUpLevelId());
            }
            if (Objects.isNull(clubMember)) {
                request.error(ErrorCode.CLUB_NOT_PROMOTION, "CClubPromotionPersonalCount CLUB_NOT_PROMOTION");
                return;
            }
        }
        //如果值传为0的时候 是推广员查看战绩列表 则手动加入pid 不影响之前的功能

        ClubMember clubMemberChaXun = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, req.pid);
        if (req.partnerPid == 0) {
            req.partnerPid = clubMemberChaXun.getClubMemberBO().getPartnerPid();
        }
        SData_Result<?> result = ClubMgr.getInstance().getClubRankMgr().getClubPromotionPersonalCount(req.clubId, req.pid, req.partnerPid);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), "ErrorCode:{%s},Msg:{%s}", result.getCode(), result.getMsg());
            return;
        }
        request.response(result.getData());
    }

}
