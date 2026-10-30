package core.network.client2game.handler.family;

import business.player.Player;
import business.player.feature.PlayerFamily;
import cenum.FamilyEnum.BindingFamilyEnum;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.family.CPlayer_BindingFamily;

import java.io.IOException;

/**
 * 玩家绑定工会
 *
 * @author Huaxing
 */
public class PlayerBindingFamily extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        //pid ，副会长
        final CPlayer_BindingFamily req = new Gson().fromJson(message, CPlayer_BindingFamily.class);
        SData_Result result = player.getFeature(PlayerFamily.class).family(req.pidStr, BindingFamilyEnum.valueOf(req.familyEnum));
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
