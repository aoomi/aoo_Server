package core.db.mgr;

import com.ddm.server.common.CommLogD;
import core.db.DataBaseMgr;
import core.db.version.DBVersionManager;

import java.sql.Connection;

/**
 * The implementation class of DB version manager and automatic update
 *
 * @change Clark
 */
public class Game_DBVersionManager extends DBVersionManager {
    public static Game_DBVersionManager getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final Game_DBVersionManager INSTANCE = new Game_DBVersionManager();
    }

    /**
     * 新版db获取连接
     *
     * @return
     */
    @Override
    public Connection getConnection() {
        try {
            return DataBaseMgr.get(getSourceName()).getConnection();
        } catch (Exception e) {
            CommLogD.error("getConnection fail:clark_game");
        }
        return null;
    }

    @Override
    public String getSourceName() {
        return "clark_game";
    }

}
