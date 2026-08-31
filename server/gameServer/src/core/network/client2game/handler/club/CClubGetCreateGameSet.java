package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.club.CClub_GetCreateGameSet;

import java.io.IOException;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class CClubGetCreateGameSet extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_GetCreateGameSet req = new Gson().fromJson(message, CClub_GetCreateGameSet.class);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.clubId);
        if (null == club) {
            request.error(ErrorCode.NotAllow, "CClubGetCreateGameSet not find club clubId = " + req.clubId);
            return;
        }
        // 检查是否亲友圈管理员
        if (!ClubMgr.getInstance().getClubMemberMgr().isMinister(req.clubId, player.getPid())) {
            request.error(ErrorCode.CLUB_NOTMINISTER, "not Minister ClubID:{%d},Pid:{%d}", req.clubId, player.getPid());
            return;
        }
        request.response(club.getCreateGameSet(player).getData());
    }

}
