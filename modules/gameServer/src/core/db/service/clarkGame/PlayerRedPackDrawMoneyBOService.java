package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerRedPackDrawMoneyBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerRedPackDrawMoneyBOService implements BaseService<PlayerRedPackDrawMoneyBO> {
    private BaseClarkGameDao<PlayerRedPackDrawMoneyBO> playerRedPackDrawMoneyBODao = new BaseClarkGameDao<>(PlayerRedPackDrawMoneyBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerRedPackDrawMoneyBODao;
    }
}

