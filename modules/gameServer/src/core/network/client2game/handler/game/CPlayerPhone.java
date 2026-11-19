package core.network.client2game.handler.game;

import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerWallet;
import cenum.ItemFlow;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.phone.PhoneEvent;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.CPlayer_Phone;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * 设置手机
 *
 * @author Huaxing
 */
public class CPlayerPhone extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message)
            throws WSException, IOException {
        final CPlayer_Phone req = new Gson().fromJson(message, CPlayer_Phone.class);
        if (req.oldPhone > 0L) {
            // 旧手机号 > 0 && 输入旧手机号和身上手机号不一致报错
            if (req.oldPhone != player.getPlayerBO().getPhone()) {
                request.error(ErrorCode.Error_Old_Phone, "ERROR_oldPhone");
                return;
            }
        } else {
            // 没有旧手机号 && 玩家本身就有手机号，不让设置
            if (player.getPlayerBO().getPhone() > 0L) {
                request.error(ErrorCode.Error_Phone, "Exist_Phone > 0L ");
                return;
            }
        }
        // 是否手机号
        if (!StringUtil.isPhone(String.valueOf(req.phone))) {
            request.error(ErrorCode.Error_Phone, "Error_Phone");
            return;
        }
        // 电话存在
        if (player.getPlayerBO().getPhone() == req.phone) {
            request.error(ErrorCode.Exist_Phone, "Exist_Phone == ");
            return;
        }
        Player wxPhone = PlayerMgr.getInstance().getPlayerPhone(req.phone);
        // 检查手机号是否存在
        if (Objects.nonNull(wxPhone)) {
            if (StringUtils.isNotEmpty(player.getPlayerBO().getWx_unionid()) && player.getPlayerBO().getWx_unionid().length() >= 20) {
                if (StringUtils.isNotEmpty(wxPhone.getPlayerBO().getWx_unionid()) && wxPhone.getPlayerBO().getWx_unionid().length() >= 20) {
                    // 手机和微信uid 一样不能进行绑定
                    request.error(ErrorCode.Exist_Phone, "Exist_Phone");
                    return;
                } else {
                    wxPhone.getPlayerBO().savePhone(0L);
                }
            } else {
                request.error(ErrorCode.Exist_Phone, "Exist_Phone");
                return;
            }
        }
        if (player.getPlayerBO().getPhone() <= 0L) {
            player.getFeature(PlayerWallet.class).gainItemFlow(GameConfig.Phone(), ItemFlow.Phone);
        }
        player.getPlayerBO().savePhone(req.phone);
        if (player.getPlayerBO().getPhone() > 0L) {
            DispatcherComponent.getInstance().publish(new PhoneEvent(player));
        }
        request.response();
    }

}
