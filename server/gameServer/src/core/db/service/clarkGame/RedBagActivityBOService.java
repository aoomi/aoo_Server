package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.RedBagActivityBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class RedBagActivityBOService implements BaseService<RedBagActivityBO> {
    private BaseClarkGameDao<RedBagActivityBO> redBagActivityBODao = new BaseClarkGameDao<>(RedBagActivityBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return redBagActivityBODao;
    }
}


