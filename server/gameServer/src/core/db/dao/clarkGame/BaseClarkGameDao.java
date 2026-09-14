package core.db.dao.clarkGame;

import com.ddm.server.annotation.Dao;
import core.db.DataBaseMgr;
import core.db.persistence.CustomerDao;

@Dao
public class BaseClarkGameDao<T> extends CustomerDao<T> {

    public BaseClarkGameDao(Class<T> clz) {
        super(clz);
    }

    public BaseClarkGameDao() {

    }

    @Override
    public DataBaseMgr getDataSource() {
        return DataBaseMgr.get("clark_game");
    }
}
