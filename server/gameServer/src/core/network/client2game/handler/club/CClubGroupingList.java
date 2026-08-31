package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.club.CClub_BanGame;

import java.io.IOException;

/**
 * 亲友圈分组列表
 *
 * @author zaf
 */
public class CClubGroupingList extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_BanGame req = new Gson().fromJson(message, CClub_BanGame.class);
        SData_Result result = ClubMgr.getInstance().getClubMemberMgr().checkClubGetMemberPromotionList(req.clubId, player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            // 获取指定的亲友圈信息
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
            if (null == club) {
                request.error(ErrorCode.NotAllow, "null == club ClubID:{%d}", req.clubId);
                return;
            }
            // 获取玩家分组列表
            request.response(club.getClubGroupingInfoList(req, player.getPid()));
        } else {
            request.error(result.getCode(), result.getMsg());
            return;
        }

    }
}
