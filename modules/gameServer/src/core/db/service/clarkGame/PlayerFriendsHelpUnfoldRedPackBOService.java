package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerFriendsHelpUnfoldRedPackBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerFriendsHelpUnfoldRedPackBOService implements BaseService<PlayerFriendsHelpUnfoldRedPackBO> {
    private BaseClarkGameDao<PlayerFriendsHelpUnfoldRedPackBO> playerFriendsHelpUnfoldRedPackBODao = new BaseClarkGameDao<>(PlayerFriendsHelpUnfoldRedPackBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerFriendsHelpUnfoldRedPackBODao;
    }
}

