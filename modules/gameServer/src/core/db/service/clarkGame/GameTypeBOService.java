package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.GameTypeBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class GameTypeBOService implements BaseService<GameTypeBO> {
    private BaseClarkGameDao<GameTypeBO> gameTypeBODao = new BaseClarkGameDao<>(GameTypeBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return gameTypeBODao;
    }
}
