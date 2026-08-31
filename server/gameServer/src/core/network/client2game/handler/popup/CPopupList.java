package core.network.client2game.handler.popup;

import business.player.Player;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 弹窗列表
 */
public class CPopupList extends PlayerHandler {

    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CPopupRequstType clientPack = new Gson().fromJson(message, CPopupRequstType.class);
        request.response(player.pushPopupList(clientPack.type).orElseGet(() -> new ArrayList<>()));
        player.setTodayFirstLogin(false);
    }

    /**
     * 接收数据
     */
    class CPopupRequstType {
        private int type = -1;

        public CPopupRequstType(int type) {
            this.type = type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }
}
