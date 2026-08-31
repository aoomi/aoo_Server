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
 * 亲友圈合伙人个人战绩
 * 创建者、管理员可以查询。
 *
 * @author Administrator
 */
public class CClubPromotionPersonalRecord extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CClub_PartnerPersonalRecord req = new Gson().fromJson(message, CClub_PartnerPersonalRecord.class);

        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, player.getPid());
        if (Objects.isNull(clubMember)) {
            request.error(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, " clubMember CClubPromotionPersonalCount CLUB_NOT_EXIST_MEMBER_INFO");
            return;
        }
        if (clubMember.isNotClubCreate() && clubMember.isNotPromotion() && clubMember.isNotLevelPromotion()) {
            if (!clubMember.isPromotionManage()) {
                request.error(ErrorCode.CLUB_NOT_PROMOTION, "CClubPromotionPersonalCount CLUB_NOT_PROMOTION");
                return;
            }
            //如果是推广员管理 重新计算权限
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
        ClubMember clubMemberChaXun = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.clubId, req.pid);
        if (Objects.isNull(clubMemberChaXun)) {
            request.error(ErrorCode.CLUB_NOT_EXIST_MEMBER_INFO, "clubMemberChaXun CClubPromotionPersonalCount CLUB_NOT_EXIST_MEMBER_INFO");
            return;
        }
        if (req.partnerPid == 0) {
            req.partnerPid = clubMemberChaXun.getClubMemberBO().getPartnerPid();
        }
        SData_Result<?> result = ClubMgr.getInstance().getClubRankMgr().getClubPromotionPersonalRecord(req.clubId, req.pid, req.partnerPid, req.pageNum);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), "ErrorCode:{%s},Msg:{%s}", result.getCode(), result.getMsg());
            return;
        }
        request.response(result.getData());
    }

}
