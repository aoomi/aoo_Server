package core.network.client2game.handler.base;

import business.player.Player;
import business.player.PlayerMgr;
import com.ddm.server.common.Config;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.ClientSession;
import core.network.client2game.handler.BaseHandler;
import core.server.ServerConfig;
import jsproto.c2s.iclass.C1002_ForceLogin;

import java.io.IOException;

/**
 * 强制登录
 * 测试模式下可用
 */
public class C1002ForceLogin extends BaseHandler {

    @Override
    public void handle(WebSocketRequest request, String message) throws IOException {
        final C1002_ForceLogin req = new Gson().fromJson(message, C1002_ForceLogin.class);
        if (Config.DE_DEBUG()) {
            int loginkey = ServerConfig.getLoginKey();
            if (loginkey == 0 || req.loginkey != loginkey) {
                request.error(ErrorCode.NotAllow, "loginkey 错误");
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayer(req.pid);
            if (player == null) {
                request.error(ErrorCode.NotAllow, "指定玩家" + req.pid + "不存在");
                return;
            }

            ClientSession session = (ClientSession) request.getSession();
            session.setValid(true);

            PlayerMgr.getInstance().connectPlayer(session, player);
            session.setAccountID(player.getAccountID());
            session.setPlayerSid(player.getPlayerBO().getSid());
            request.response();
        } else {
            request.error(ErrorCode.Banned_Login, "Banned_Login");
        }
    }
}
