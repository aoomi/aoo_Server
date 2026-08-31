package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.EMailBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class EMailBOService implements BaseService<EMailBO> {
    private BaseClarkGameDao<EMailBO> eMailBODao = new BaseClarkGameDao<>(EMailBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return eMailBODao;
    }
}

