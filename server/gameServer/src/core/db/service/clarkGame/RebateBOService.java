package core.db.service.clarkGame;

import com.ddm.server.annotation.Autowired;
import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.RebateBODao;
import core.db.entity.clarkGame.RebateBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class RebateBOService implements BaseService<RebateBO> {
    @Autowired
    private RebateBODao rebateBODao;

    public int RebateSumFlag(long accountID) {
        return rebateBODao.RebateSumFlag(accountID);
    }

    public Long sum(Criteria criteria, String property) {
        return rebateBODao.sum(criteria, property);
    }

    @Override
    public CustomerDao getDefaultDao() {
        return rebateBODao;
    }
}



