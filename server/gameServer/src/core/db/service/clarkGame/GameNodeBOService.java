package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.GameNodeBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class GameNodeBOService implements BaseService<GameNodeBO> {
    private BaseClarkGameDao<GameNodeBO> gameNodeBOBaseClarkGameDao = new BaseClarkGameDao<>(GameNodeBO.class);
    @Override
    public CustomerDao getDefaultDao() {
        return gameNodeBOBaseClarkGameDao;
    }
}
