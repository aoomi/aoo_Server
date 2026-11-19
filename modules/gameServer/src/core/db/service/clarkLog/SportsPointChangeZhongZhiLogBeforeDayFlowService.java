package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.SportsPointChangeZhongZhiLogBeforeDayFlow;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class SportsPointChangeZhongZhiLogBeforeDayFlowService implements BaseService<SportsPointChangeZhongZhiLogBeforeDayFlow> {
    private BaseClarkLogDao<SportsPointChangeZhongZhiLogBeforeDayFlow> baseClarkLogDao = new BaseClarkLogDao<>(SportsPointChangeZhongZhiLogBeforeDayFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return baseClarkLogDao;
    }

    public SportsPointChangeZhongZhiLogBeforeDayFlow findOne(long id) {
        return baseClarkLogDao.findOne(Restrictions.and(Restrictions.eq("id", id)), null, null);
    }

}




