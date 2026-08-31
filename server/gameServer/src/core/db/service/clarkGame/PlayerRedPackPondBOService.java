package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerRedPackPondBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerRedPackPondBOService implements BaseService<PlayerRedPackPondBO> {
    private BaseClarkGameDao<PlayerRedPackPondBO> playerRedPackPondBODao = new BaseClarkGameDao<>(PlayerRedPackPondBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerRedPackPondBODao;
    }
}

