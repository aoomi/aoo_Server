/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.network.client2game;

import com.ddm.server.loader.JarLoaderMgr;
import com.ddm.server.websocket.BaseAcceptor;
import com.ddm.server.websocket.BaseIoHandler;
import com.ddm.server.websocket.handler.IBaseHandler;
import core.network.netty.NettyServerHandler;
import org.apache.mina.core.session.IoSession;

import java.util.Set;

public class ClientAcceptor extends BaseAcceptor<ClientSession> {

    public static class ClientAcceptorIoHandler extends BaseIoHandler<ClientSession> {

        @Override
        public ClientSession createSession(IoSession session, long sessionID) {
            return new ClientSession(session, sessionID);
        }
    }

    private static ClientAcceptor _instance = new ClientAcceptor();

    public static final ClientAcceptor getInstance() {
        return _instance;
    }

    private ClientHandlerDispatcher dispatcher = null;

    /**
     * 初始化
     */
    public void init(int type) {
        if (type == 1) {//mina
            initSocket(type, new ClientAcceptorIoHandler());
            this.dispatcher = new ClientHandlerDispatcher();
            this.dispatcher.init();
            this.handler.setMessageDispatcher(dispatcher);
        } else { //netty
            initSocket(type, new NettyServerHandler());
            this.dispatcher = new ClientHandlerDispatcher();
            this.dispatcher.init();
            this.serverHandler.setMessageDispatcher(dispatcher);
        }
        this.loadHandLer(JarLoaderMgr.getInstance().getHandlers());
    }

    /**
     * 加载包头部
     *
     * @param pack 指定包
     */
    public void loadHandLer(Set<String> dealers) {
        this.dispatcher.registerRequestHandlers(dealers);
    }

    /**
     * 加载包头部
     *
     * @param pack 指定包
     */
    public void loadHandLer(String name) {
        this.dispatcher.registerRequestHandlers(name);
    }

    public IBaseHandler getHandle(String handle) {
        return this.dispatcher.getHandle(handle);
    }

}
