package core.db.dao.clarkGame;

import com.ddm.server.annotation.Dao;
import core.db.entity.clarkGame.PlayerBO;
import core.db.persistence.Repository;

@Dao(dataSource = "clark_game")
public interface IBaseClarkGameDao extends Repository<PlayerBO> {

}
