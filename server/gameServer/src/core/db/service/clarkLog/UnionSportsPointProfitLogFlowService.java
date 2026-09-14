package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.UnionSportsPointProfitLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class UnionSportsPointProfitLogFlowService implements BaseService<UnionSportsPointProfitLogFlow> {
    private BaseClarkLogDao<UnionSportsPointProfitLogFlow> unionSportsPointProfitLogFlowBaseClarkLogDao = new BaseClarkLogDao<>(UnionSportsPointProfitLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return unionSportsPointProfitLogFlowBaseClarkLogDao;
    }


}




