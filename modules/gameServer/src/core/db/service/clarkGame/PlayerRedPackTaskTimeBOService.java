package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerRedPackTaskTimeBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerRedPackTaskTimeBOService implements BaseService<PlayerRedPackTaskTimeBO> {
    private BaseClarkGameDao<PlayerRedPackTaskTimeBO> playerRedPackTaskTimeBODao = new BaseClarkGameDao<>(PlayerRedPackTaskTimeBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerRedPackTaskTimeBODao;
    }
}

