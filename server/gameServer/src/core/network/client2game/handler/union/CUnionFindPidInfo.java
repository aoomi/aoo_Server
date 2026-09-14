package core.network.client2game.handler.union;

import business.player.Player;
import business.player.PlayerMgr;
import business.global.union.UnionMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.ClubPromotionPidInfo;
import jsproto.c2s.iclass.union.CUnion_BanGamePlayer;

import java.io.IOException;
import java.util.Objects;

/**
 * 查询人员
 */
public class CUnionFindPidInfo extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        CUnion_BanGamePlayer req = new Gson().fromJson(message, CUnion_BanGamePlayer.class);
        SData_Result permission = UnionMgr.getInstance().getUnionMemberMgr()
                .checkBanGamePermission(req, player.getPid());
        if (!ErrorCode.Success.equals(permission.getCode())) {
            request.error(permission.getCode(), permission.getMsg());
            return;
        }
        Player findPlayer = PlayerMgr.getInstance().getPlayer(req.getPid());
        SData_Result result;
        if (Objects.isNull(findPlayer)) {
            result = SData_Result.make(ErrorCode.Player_PidError, "Player_PidError");
        } else {
            result = SData_Result.make(ErrorCode.Success, new ClubPromotionPidInfo(findPlayer.getShortPlayer(), 2));
        }
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
