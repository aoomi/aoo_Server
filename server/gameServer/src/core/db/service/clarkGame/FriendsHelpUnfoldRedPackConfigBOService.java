package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.FriendsHelpUnfoldRedPackConfigBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class FriendsHelpUnfoldRedPackConfigBOService implements BaseService<FriendsHelpUnfoldRedPackConfigBO> {
    private BaseClarkGameDao<FriendsHelpUnfoldRedPackConfigBO> friendsHelpUnfoldRedPackConfigBODao = new BaseClarkGameDao<>(FriendsHelpUnfoldRedPackConfigBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return friendsHelpUnfoldRedPackConfigBODao;
    }
}

