package core.network.client2game.handler.base;

import BaseCommon.CommLog;
import business.global.GM.MaintainServerMgr;
import business.global.secret.SecretManager;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.DefaultEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.def.SubscribeEnum;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.ClientSession;
import core.network.client2game.handler.BaseHandler;
import jsproto.c2s.iclass.C1009_ResetLogin;
import jsproto.c2s.iclass.S1009_ResetLogin;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class C1009ResetLogin extends BaseHandler {

    @Override
    public void handle(WebSocketRequest request, String data) throws IOException {
        final C1009_ResetLogin req = new Gson().fromJson(data, C1009_ResetLogin.class);
        ClientSession session = (ClientSession) request.getSession();
        if (PlayerMgr.getInstance().havePlayerByAccountID(req.accountID) && session.getAccountID() == req.accountID) {
            session.setValid(true);
            Player player = PlayerMgr.getInstance().getPlayerByAccountID(req.accountID);
            // 检查是否处于维护中
            if (!MaintainServerMgr.getInstance().checkUnderMaintenance(request, player)) {
                // 维护中
                return;
            }
            if (player.isBannedLogin(request)) {
                return;
            }
            // 连接账号玩家
            PlayerMgr.getInstance().connectPlayer(session, player);
            String serverToken = UUID.randomUUID().toString().replaceAll("-", "");
            SecretManager.saveC1009ResetLogin(req.accountID, serverToken);
            String subjectTopic = SubscribeEnum.HALL.name();
            if (req.serverType == 1) {
                // 检查玩家是否存在游戏
                SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(player.getPid());
                if (Objects.nonNull(sharePlayer)) {
                    ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByRoomId(sharePlayer.getRoomInfo().getRoomId());
                    if (Objects.isNull(shareRoom)) {
                        // 强制清空用户的游戏状态
                        player.onGMExitRoom();
                    } else {
                        subjectTopic = sharePlayer.getRoomInfo().getSubjectTopic();
                    }
                }
            }
            request.response(S1009_ResetLogin.make(CommTime.nowMS(), CommTime.timezone().getRawOffset(), CommTime.nowSecond(), DefaultEnum.FAMILY_ID.value(), (byte) 0, req.accountID, 0, serverToken), subjectTopic);
        } else {
            CommLog.error("[C1009ResetLogin] account mismatch accountId:{},sessionAccountId:{}", req.accountID, session.getAccountID());
            request.error(ErrorCode.KickOut_AccountTokenError.value(), "AccountIdNotExist");
        }
    }


}
