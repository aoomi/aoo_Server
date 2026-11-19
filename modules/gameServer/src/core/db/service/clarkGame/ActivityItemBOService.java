package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ActivityItemBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class ActivityItemBOService implements BaseService<ActivityItemBO> {
    private BaseClarkGameDao<ActivityItemBO> activityItemBODao = new BaseClarkGameDao<>(ActivityItemBO.class);

    @Override
    public CustomerDao<ActivityItemBO> getDefaultDao() {
        return activityItemBODao;
    }
}


