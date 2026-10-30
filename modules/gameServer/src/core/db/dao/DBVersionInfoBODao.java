package core.db.dao;

import com.ddm.server.annotation.Autowired;
import com.ddm.server.annotation.Dao;
import core.db.DataBaseMgr;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.DBVersionInfoBO;

import java.util.List;
import java.util.Map;

@Dao
public class DBVersionInfoBODao extends BaseClarkGameDao<DBVersionInfoBO> {

    private String sourceName = "clark_game";

    @Autowired
    private DBVersionInfoBO dbVersionInfoBO;

    public DBVersionInfoBO getById(int id) {
        return getBean("select * from " + getTableName() + " where id = ?", id);
    }

    public List<DBVersionInfoBO> listAllDBVersionInfoBO() {
        return listBean("select * from " + getTableName());
    }

    public int createTableSql() {
        return this.execute(dbVersionInfoBO.createTableSql());
    }

    public List<Map<String, Object>> showTables() {
        return listMap("show TABLES;");
    }

    public String existTable() {
        return getValue("show tables like '" + getTableName() + "'");
    }

    public List<Map<String, Object>> desc(String tableName) {
        return listMap("desc " + tableName + ";");
    }

    public int executeSql(String sql, Object... args) {
        return this.execute(sql, args);
    }

    public DBVersionInfoBODao() {
    }

    @Override
    public DataBaseMgr getDataSource() {
        return DataBaseMgr.get(sourceName);
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }


}
