package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.GameSetBO;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class GameSetBOService implements BaseService<GameSetBO> {
    private BaseClarkGameDao<GameSetBO> gameSetBODao = new BaseClarkGameDao<>(GameSetBO.class);

    public GameSetBO findOne(long roomID, int setID) {
        return gameSetBODao.findOne(Restrictions.and(Restrictions.eq("roomID", roomID), Restrictions.eq("setID", setID)), null, null);
    }

    @Override
    public CustomerDao getDefaultDao() {
        return gameSetBODao;
    }
}
