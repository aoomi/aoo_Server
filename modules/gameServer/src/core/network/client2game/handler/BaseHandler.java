package core.network.client2game.handler;

import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.RequestHandler;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.ClientSession;

import java.io.IOException;

/**
 *
 */
public abstract class BaseHandler extends RequestHandler {
    public String getOpName() {
        return this.getClass().getSimpleName();
    }

    public BaseHandler() {
        super();
    }

    public BaseHandler(short opCode, String opName) {
        super(opCode, opName);
    }

    @Override
    public void handleMessage(final WebSocketRequest request, final String data) throws WSException, IOException {
        if (!(request.getSession() instanceof ClientSession)) {
            CommLogD.warn("{} not handled.", request.getHeader().event);
            return;
        }

        // 加解密处理
        try {
            CommLogD.info("request client sessionID：{}, Event：{}, Interface：{}", request.getSession().getSessionId(), request.getHeader().event, getOpName());
            handle(request, data);
        } catch (Throwable e) {
            CommLogD.error(this.getClass().getName() + " Exception: ", e);
            request.error(ErrorCode.NotAllow, "服务端处理请求失败");
        }
    }

    public abstract void handle(final WebSocketRequest request, final String message) throws IOException;
}
