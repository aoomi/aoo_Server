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
public class Zle_DBVersionManager extends DBVersionManager {
    public static Zle_DBVersionManager getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final Zle_DBVersionManager INSTANCE = new Zle_DBVersionManager();
    }

    @Override
    public Connection getConnection() {
        try {
            return DataBaseMgr.get(getSourceName()).getConnection();
        } catch (Exception e) {
            CommLogD.error("getConnection fail:db_zle");
        }
        return null;
    }

    @Override
    public String getSourceName() {
        return "db_zle";
    }
}
