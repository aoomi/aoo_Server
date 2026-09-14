package core.db.dao.clarkGame;

import com.ddm.server.annotation.Dao;
import core.db.entity.clarkGame.UnionRoomConfigScorePercentBO;
import core.db.persistence.Repository;

@Dao(dataSource = "clark_game")
public interface UnionRoomConfigScorePercentBODao extends Repository<UnionRoomConfigScorePercentBO> {


}
