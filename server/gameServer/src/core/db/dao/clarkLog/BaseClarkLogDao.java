package core.db.dao.clarkLog;

import core.db.DataBaseMgr;
import core.db.persistence.CustomerDao;

public class BaseClarkLogDao<T> extends CustomerDao<T> {

    public BaseClarkLogDao(Class<T> clz) {
        super(clz);
    }

    public BaseClarkLogDao() {

    }

    @Override
    public DataBaseMgr getDataSource() {
        return DataBaseMgr.get("clark_log");
    }
}
