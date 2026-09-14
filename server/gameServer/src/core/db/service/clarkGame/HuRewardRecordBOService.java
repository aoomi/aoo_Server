package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.HuRewardRecordBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class HuRewardRecordBOService implements BaseService<HuRewardRecordBO> {
    private BaseClarkGameDao<HuRewardRecordBO> huRewardRecordBODao = new BaseClarkGameDao<>(HuRewardRecordBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return huRewardRecordBODao;
    }
}
