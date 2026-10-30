package core.network.netty;

import com.ddm.server.netty.ServerHandler;
import core.network.client2game.ClientSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class NettyServerHandler extends ServerHandler<ClientSession> {
    @Override
    public ClientSession createSession(Channel session, long sessionID) {
        return new ClientSession(session, sessionID);
    }
}
