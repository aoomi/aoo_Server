package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerPlayBackBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerPlayBackBOService implements BaseService<PlayerPlayBackBO> {
    private BaseClarkGameDao<PlayerPlayBackBO> playerPlayBackBODao = new BaseClarkGameDao<>(PlayerPlayBackBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerPlayBackBODao;
    }
}
