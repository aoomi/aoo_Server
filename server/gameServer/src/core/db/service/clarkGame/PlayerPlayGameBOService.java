package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerPlayGameBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerPlayGameBOService implements BaseService<PlayerPlayGameBO> {
    private BaseClarkGameDao<PlayerPlayGameBO> playerPlayGameBODao = new BaseClarkGameDao<>(PlayerPlayGameBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerPlayGameBODao;
    }
}

