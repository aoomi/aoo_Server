package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerGPSBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerGPSBOService implements BaseService<PlayerGPSBO> {
    private BaseClarkGameDao<PlayerGPSBO> playerGPSBODao = new BaseClarkGameDao<>(PlayerGPSBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerGPSBODao;
    }
}


