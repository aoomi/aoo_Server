package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ClubListBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class ClubListBOService implements BaseService<ClubListBO> {
    private BaseClarkGameDao<ClubListBO> clubListBODao = new BaseClarkGameDao<>(ClubListBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubListBODao;
    }
}

