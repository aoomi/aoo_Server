package core.db.dao.clarkLog;

import com.ddm.server.annotation.Dao;
import core.db.entity.DBVersionInfoBO;
import core.db.persistence.Repository;

@Dao(dataSource = "clark_log")
public interface IBaseClarkLogDao extends Repository<DBVersionInfoBO> {

}