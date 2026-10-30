package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ZleRechargeBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class ZleRechargeBOService implements BaseService<ZleRechargeBO> {
    private BaseClarkGameDao<ZleRechargeBO> zleRechargeBODao = new BaseClarkGameDao<>(ZleRechargeBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return zleRechargeBODao;
    }
}


