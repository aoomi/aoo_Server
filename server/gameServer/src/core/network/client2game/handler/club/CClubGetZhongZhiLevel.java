package core.network.client2game.handler.club;

import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.ClubZhongZhiLevel;
import jsproto.c2s.iclass.club.CClub_FindPIDAdd;

import java.io.IOException;
import java.util.Objects;

/**
 * 获取中至战队等级
 */
public class CClubGetZhongZhiLevel extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CClub_FindPIDAdd req = new Gson().fromJson(message, CClub_FindPIDAdd.class);
        ClubMember clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(req.getClubId(), player.getPid());
        if (Objects.isNull(clubMember)) {
            request.response(SData_Result.make(ErrorCode.CLUB_NOTCLUBMEMBER, "CLUB_NOTCLUBMEMBER"));
            return;
        }
        request.response(new ClubZhongZhiLevel(clubMember.getClubMemberBO().getLevelZhongZhi()));
    }
}
