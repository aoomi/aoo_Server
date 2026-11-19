package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ClubGroupingBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class ClubGroupingBOService implements BaseService<ClubGroupingBO> {
    private BaseClarkGameDao<ClubGroupingBO> clubGroupingBODao = new BaseClarkGameDao<>(ClubGroupingBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubGroupingBODao;
    }
}

