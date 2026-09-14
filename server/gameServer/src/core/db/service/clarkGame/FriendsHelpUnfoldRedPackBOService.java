package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.FriendsHelpUnfoldRedPackBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class FriendsHelpUnfoldRedPackBOService implements BaseService<FriendsHelpUnfoldRedPackBO> {
    private BaseClarkGameDao<FriendsHelpUnfoldRedPackBO> friendsHelpUnfoldRedPackBODao = new BaseClarkGameDao<>(FriendsHelpUnfoldRedPackBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return friendsHelpUnfoldRedPackBODao;
    }
}

