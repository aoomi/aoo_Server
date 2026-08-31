package core.network.client2game.handler.base;

import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.BaseHandler;

import java.io.IOException;

public class C1003HeartBeat extends BaseHandler {

    @Override
    public void handle(WebSocketRequest request, String message) throws IOException {
        request.response();
    }
}
