package core.db.service;

import com.ddm.server.annotation.Autowired;
import com.ddm.server.annotation.Service;
import core.db.dao.DBVersionInfoBODao;
import core.db.entity.DBVersionInfoBO;
import core.db.persistence.CustomerDao;

import java.util.List;
import java.util.Map;

@Service(source = "clark_game")
public class DBVersionInfoBOService implements BaseService<DBVersionInfoBO> {
    @Autowired
    private DBVersionInfoBODao dbVersionInfoBODao;

    /**
     * 创建数据表
     */
    public void createTableSql() {
        dbVersionInfoBODao.createTableSql();
    }

    public String existTable() {
        return dbVersionInfoBODao.existTable();
    }

    public List<Map<String, Object>> showTables() {
        return dbVersionInfoBODao.showTables();
    }

    public List<Map<String, Object>> desc(String table) {
        return dbVersionInfoBODao.desc(table);
    }

    public int execute(String sql) {
        return dbVersionInfoBODao.executeSql(sql);
    }

    public void setSourceName(String sourceName) {
        dbVersionInfoBODao.setSourceName(sourceName);
    }

    @Override
    public String toString() {
        return "DBVersionInfoBOService";
    }

    @Override
    public CustomerDao<DBVersionInfoBO> getDefaultDao() {
        return dbVersionInfoBODao;
    }
}
