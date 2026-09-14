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
public class Log_DBVersionManager extends DBVersionManager {
    public static Log_DBVersionManager getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final Log_DBVersionManager INSTANCE = new Log_DBVersionManager();
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
            CommLogD.error("getConnection fail:clark_log");
        }
        return null;
    }


    @Override
    public String getSourceName() {
        return "clark_log";
    }
}
