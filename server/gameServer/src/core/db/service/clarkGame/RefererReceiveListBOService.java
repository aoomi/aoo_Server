package core.db.service.clarkGame;

import com.ddm.server.annotation.Autowired;
import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.RefererReceiveListBODao;
import core.db.entity.clarkGame.RefererReceiveListBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

import java.util.Map;

@Service(source = "clark_game")
public class RefererReceiveListBOService implements BaseService<RefererReceiveListBO> {
    @Autowired
    private RefererReceiveListBODao refererReceiveListBODao;

    public Long count(Criteria criteria) {
        return refererReceiveListBODao.count(criteria);
    }

    public Map<String, Object> findRefererReceiveTotalBO(Criteria criteria) {
        return refererReceiveListBODao.findRefererReceiveTotalBO(criteria);
    }

    @Override
    public CustomerDao getDefaultDao() {
        return refererReceiveListBODao;
    }
}


