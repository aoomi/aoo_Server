package core.network.client2game.handler.base;

import business.global.GM.MaintainServerMgr;
import business.player.Player;
import business.player.PlayerMgr;
import cenum.DefaultEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.http.client.HttpAsyncClient;
import com.ddm.server.http.client.IResponseHandler;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.network.client2game.ClientSession;
import core.network.client2game.handler.BaseHandler;
import core.server.OpenSeverTime;
import jsproto.c2s.iclass.C1007_LoginPhone;
import jsproto.c2s.iclass.S1004_Login;

import java.io.IOException;
import java.util.UUID;

public class C1007LoginPhone extends BaseHandler {

    @Override
    public void handle(WebSocketRequest request, String data) throws IOException {
        final C1007_LoginPhone req = new Gson().fromJson(data, C1007_LoginPhone.class);
        // 是否手机号
        if (!StringUtil.isPhone(String.valueOf(req.phone))) {
            request.error(ErrorCode.Error_Phone, "Error_Phone");
            return;
        }
        // 人数限制
        int onlineCount = PlayerMgr.getInstance().getOnlinePlayers().size();
        if (onlineCount >= 20000) {
            request.error(ErrorCode.ErrorSysMsg, "World_MaxOnLine");
            return;
        }
        String url = String.format(System.getProperty("PhoneServerUrl"), req.phone, req.code);
        HttpAsyncClient.startHttpGet(url, new IResponseHandler() {

            @Override
            public void failed(Exception exception) {
                CommLogD.error("手机号认证服务请求失败：{}", exception.getClass().getSimpleName());
                login(request, "{\"code\":4}", req);
            }

            @Override
            public void compeleted(String response) {
                login(request, response, req);
            }
        });

    }

    private void login(WebSocketRequest request, String resultString, C1007_LoginPhone req) {
        try {
            JsonObject resJson = new JsonParser().parse(resultString).getAsJsonObject();
            int code = resJson.get("code").getAsInt();
            if (code == -1) {
                request.error(ErrorCode.Error_Code, resJson.get("msg").getAsString());
                return;
            }
            if (code != 0) {
                request.error(ErrorCode.KickOut_AccountAuthorizationFail, code + "");
                return;
            }
            Player player = PlayerMgr.getInstance().getPlayerPhone(req.phone);
            if (null == player) {
                request.error(ErrorCode.Not_Exist_Phone, "Not_Exist_Phone");
                return;
            }
            // 检查是否处于维护中
            if (!MaintainServerMgr.getInstance().checkUnderMaintenance(request, player)) {
                // 维护中
                return;
            }
            ClientSession session = (ClientSession) request.getSession();
            session.setValid(true);
            session.setAccountID(player.getAccountID());
            session.setPlayerSid(player.getPlayerBO().getSid());
            session.setWxUnionid(player.getPlayerBO().getWx_unionid());
            PlayerMgr playerMgr = PlayerMgr.getInstance();
            if (player.isBannedLogin(request)) {
                return;
            }
            // 设置UUID
            player.setUUID(UUID.randomUUID().toString().replaceAll("-", ""));
            playerMgr.connectPlayer(session, player);
            S1004_Login resultInfo = new S1004_Login();
            resultInfo.time = CommTime.nowMS();
            resultInfo.timeZone = CommTime.timezone().getRawOffset();
            resultInfo.startServerTime = OpenSeverTime.getInstance().getStartServerTime();
            resultInfo.defaultFamilyID = DefaultEnum.FAMILY_ID.value();
            resultInfo.isNeedCreateRole = 0;
            resultInfo.accountID = player.getAccountID();


            // 发送前台
            request.response(resultInfo);

        } catch (Exception e) {
            CommLogD.error("手机号认证服务回包解析失败", e);
            request.error(ErrorCode.KickOut_AccountAuthorizationFail, "");
        }
    }


}
