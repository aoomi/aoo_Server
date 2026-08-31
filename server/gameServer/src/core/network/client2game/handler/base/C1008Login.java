package core.network.client2game.handler.base;

import business.global.GM.MaintainServerMgr;
import business.player.Player;
import business.player.PlayerMgr;
import cenum.DefaultEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.http.server.HttpUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.ClientSession;
import core.network.client2game.handler.BaseHandler;
import core.server.OpenSeverTime;
import jsproto.c2s.iclass.C1008_Login;
import jsproto.c2s.iclass.S1008_Login;

import java.io.IOException;
import java.util.UUID;

public class C1008Login extends BaseHandler {

    @Override
    public void handle(WebSocketRequest request, String data) throws IOException {
        final C1008_Login req = new Gson().fromJson(data, C1008_Login.class);
        Player reconnectPlayer = PlayerMgr.getInstance().getPlayerByAccountID(req.accountID);
        if (reconnectPlayer == null || org.apache.commons.lang3.StringUtils.isEmpty(reconnectPlayer.getUUID())) {
            request.error(ErrorCode.KickOut_AccountAuthorizationFail, "重连凭据不存在，请重新登录");
            return;
        }
        try {
            HttpUtils.abstractGMParams(data, reconnectPlayer.getUUID());
        } catch (Exception e) {
            request.error(ErrorCode.NotAllow, "验签错误:{}", e.getMessage());
            return;
        }
        String version = req.version;
        // 检查版本号是否正确。
        if (!"1.0.1".equalsIgnoreCase(version)) {
            request.error(ErrorCode.KickOut_ClientVersion, String.format("版本号错误(%s)!=(1.0.1)", version));
            return;
        }
        // 人数限制
        int onlineCount = PlayerMgr.getInstance().getOnlinePlayers().size();
        if (onlineCount >= 20000) {
            request.error(ErrorCode.ErrorSysMsg, "World_MaxOnLine");
            return;
        }
        // 重连
        login(request, req);
    }

    /**
     * @param request
     * @param req
     */
    private void login(WebSocketRequest request, C1008_Login req) {

        try {
            ClientSession session = (ClientSession) request.getSession();
            session.setValid(true);
            session.setAccountID(req.accountID);
            PlayerMgr playerMgr = PlayerMgr.getInstance();

            byte isNeedCreateRole;
            // 如果已经存在player实例
            if (playerMgr.havePlayerByAccountID(req.accountID)) {
                Player player = playerMgr.getPlayerByAccountID(req.accountID);
                // 检查是否处于维护中
                if (!MaintainServerMgr.getInstance().checkUnderMaintenance(request, player)) {
                    // 维护中
                    return;
                }
                if (player.isBannedLogin(request)) {
                    return;
                }
                // 连接账号玩家
                playerMgr.connectPlayer(session, player);
                player.setUUID(UUID.randomUUID().toString().replaceAll("-", ""));
                isNeedCreateRole = 0;
            }
            // 是新建账号登录
            else {
                request.error(ErrorCode.ErrorSysMsg, "用户不存在");
                return;
            }
            S1008_Login resultInfo = new S1008_Login();
            resultInfo.time = CommTime.nowMS();
            resultInfo.timeZone = CommTime.timezone().getRawOffset();
            resultInfo.startServerTime = OpenSeverTime.getInstance().getStartServerTime();
            resultInfo.defaultFamilyID = DefaultEnum.FAMILY_ID.value();
            resultInfo.isNeedCreateRole = isNeedCreateRole;
            // 发送前台
            request.response(resultInfo);
        } catch (Exception e) {
            CommLogD.error("账号服务器({})返回({}),验证解析失败", e);
            request.error(ErrorCode.KickOut_AccountAuthorizationFail, "重连失败");
        }
    }
}
