package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.UnionMatchLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class UnionMatchLogFlowService implements BaseService<UnionMatchLogFlow> {
    private BaseClarkLogDao<UnionMatchLogFlow> unionMatchLogFlowDao = new BaseClarkLogDao<>(UnionMatchLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return unionMatchLogFlowDao;
    }


}

