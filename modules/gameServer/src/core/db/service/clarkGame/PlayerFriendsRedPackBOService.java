package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerFriendsRedPackBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerFriendsRedPackBOService implements BaseService<PlayerFriendsRedPackBO> {
    private BaseClarkGameDao<PlayerFriendsRedPackBO> playerFriendsRedPackBODao = new BaseClarkGameDao<>(PlayerFriendsRedPackBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerFriendsRedPackBODao;
    }
}

