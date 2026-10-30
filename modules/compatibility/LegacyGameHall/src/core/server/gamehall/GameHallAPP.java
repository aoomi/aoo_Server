package core.server.gamehall;


import com.ddm.server.common.ehcache.EhCacheFactory;
import core.server.GameServer;

/**
 * 大厅启动项
 *
 * @author Administrator
 */
public class GameHallAPP {
    public static void main(String[] args) throws Exception {
        EhCacheFactory.setGameId("-1");
        GameServer app = new GameServer();
        app.init(args);
    }
}
