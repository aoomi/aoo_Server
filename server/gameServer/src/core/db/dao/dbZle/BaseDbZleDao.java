package core.db.dao.dbZle;

import core.db.DataBaseMgr;
import core.db.persistence.CustomerDao;

public class BaseDbZleDao<T> extends CustomerDao<T> {

    public BaseDbZleDao(Class<T> clz) {
        super(clz);
    }

    public BaseDbZleDao() {

    }

    @Override
    public DataBaseMgr getDataSource() {
        return DataBaseMgr.get("db_zle");
    }
}
