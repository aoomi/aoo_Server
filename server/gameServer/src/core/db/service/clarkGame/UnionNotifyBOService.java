package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.UnionNotifyBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class UnionNotifyBOService implements BaseService<UnionNotifyBO> {
    private BaseClarkGameDao<UnionNotifyBO> unionNotifyBODao = new BaseClarkGameDao<>(UnionNotifyBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return unionNotifyBODao;
    }
}
