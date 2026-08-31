package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.HuRewardConfigBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class HuRewardConfigBOService implements BaseService<HuRewardConfigBO> {
    private BaseClarkGameDao<HuRewardConfigBO> huRewardConfigBODao = new BaseClarkGameDao<>(HuRewardConfigBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return huRewardConfigBODao;
    }
}

