package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.RefererShareBO;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

import java.util.List;

@Service(source = "clark_game")
public class RefererShareBOService implements BaseService<RefererShareBO> {
    private BaseClarkGameDao<RefererShareBO> refererShareBODao = new BaseClarkGameDao<>(RefererShareBO.class);

    public List<RefererShareBO> findTodayAll(long pid) {
        Criteria criteria = Restrictions.and(Restrictions.eq("pid", pid), Restrictions.addRawWhere("to_days(from_unixtime(createTime)) = to_days(now())"));
        criteria.setLimit(1);
        return refererShareBODao.findAll(criteria, null, null);
    }

    @Override
    public CustomerDao getDefaultDao() {
        return refererShareBODao;
    }
}



