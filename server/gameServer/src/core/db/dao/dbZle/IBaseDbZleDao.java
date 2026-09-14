package core.db.dao.dbZle;

import com.ddm.server.annotation.Dao;
import core.db.entity.DBVersionInfoBO;
import core.db.persistence.Repository;

@Dao(dataSource = "db_zle")
public interface IBaseDbZleDao extends Repository<DBVersionInfoBO> {

}