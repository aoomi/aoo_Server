package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerArenaBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerArenaBOService implements BaseService<PlayerArenaBO> {
    private BaseClarkGameDao<PlayerArenaBO> playerArenaBODao = new BaseClarkGameDao<>(PlayerArenaBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerArenaBODao;
    }
}