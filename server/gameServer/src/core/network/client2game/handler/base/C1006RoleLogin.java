package core.network.client2game.handler.base;

import business.global.GM.MaintainServerMgr;
import business.player.Player;
import business.player.feature.PlayerBase;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.ClientSession;
import core.network.client2game.handler.BaseHandler;
import jsproto.c2s.iclass.C1006_RoleLogin;

import java.io.IOException;

public class C1006RoleLogin extends BaseHandler {


    @Override
    public void handle(WebSocketRequest request, String data) throws IOException {

        final C1006_RoleLogin req = new Gson().fromJson(data, C1006_RoleLogin.class);
        long accountID = req.accountID;

        ClientSession session = (ClientSession) request.getSession();
        Player player = session.getPlayer();
        long sessionAccountID = session.getAccountID();
        if (sessionAccountID != accountID) {
            request.error(ErrorCode.NotAllow, "角色登录失败,session绑定账号ID不同");
            CommLogD.error("登录账号({}) != session绑定账号ID({})", accountID, sessionAccountID);
            return;
        }
        if (player == null) {
            request.error(ErrorCode.NotAllow, "角色登录失败,账号未授权登录过");
            CommLogD.error("账号({})没有login", sessionAccountID);
            return;
        }
        // 检查是否处于维护中
        if (!MaintainServerMgr.getInstance().checkUnderMaintenance(request, player)) {
            // 维护中
            return;
        }
        request.response(player.getFeature(PlayerBase.class).fullInfo(true));

    }


}
