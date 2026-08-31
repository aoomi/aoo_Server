package core.network.client2game.handler.union;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.feature.PlayerFamily;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.FamilyItem;
import jsproto.c2s.iclass.union.CUnion_Create;

import java.io.IOException;

/**
 * 创建赛事
 *
 * @author
 */
public class CUnionCreate extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {

        final CUnion_Create req = new Gson().fromJson(message, CUnion_Create.class);
        // 检查是否公会代理
        SData_Result result = player.getFeature(PlayerFamily.class).checkFamilyOwner();
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        }

        FamilyItem item = (FamilyItem) result.getData();
        if (item.getPower() != 1) {
            request.error(ErrorCode.FAMILY_POWER_ERROR, "FAMILY_POWER_ERROR");
            return;
        }

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubId());
        if (req.getClubId() <= 0 || null == req) {
            request.error(ErrorCode.CLUB_NOT_EXIST, "CUnionCreate CLUB_NOT_EXIST");
            return;
        }
        // 不是亲友圈创建者
        if (club.getClubListBO().getOwnerID() != player.getPid()) {
            request.error(ErrorCode.CLUB_NOT_CREATE, "CUnionCreate CLUB_NOT_CREATE :{%d}", club.getClubListBO().getOwnerID());
            return;
        }
        result = UnionMgr.getInstance().getUnionListMgr().onUnionCreateTest(req, club, player);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.error(result.getCode(), result.getMsg());
            return;
        } else {
            request.response();
        }
    }

}
