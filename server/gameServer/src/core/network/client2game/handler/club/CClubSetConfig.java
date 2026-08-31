package core.network.client2game.handler.club;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.cclass.club.ClubConfig;
import jsproto.c2s.iclass.club.CClub_SetConfig;

import java.io.IOException;

/**
 * 设置亲友圈配置
 *
 * @author zaf
 */
public class CClubSetConfig extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CClub_SetConfig req = new Gson().fromJson(message, CClub_SetConfig.class);
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(req.getClubID());
        // 检查亲友圈是否存在。
        if (null == club) {
            request.error(ErrorCode.NotAllow, "CClubGetConfig null == club");
            return;
        }

        // 请联系代理前往后台，绑定亲友圈创建者
        if (club.getClubListBO().getOwnerID() != player.getPid()) {
            request.error(ErrorCode.CLUB_NOT_CREATE, "CClubPartnerFindInfo CLUB_NOT_CREATE");
            return;
        }
        club.getClubListBO().saveClubConfig(new ClubConfig(req.getBasics(), req.getKickOutRoom(), req.getDissolveSet(), req.getDissolveTime()));
        // 亲友圈配置。
        request.response(club.getClubListBO().getClubConfigJson());
    }

}
