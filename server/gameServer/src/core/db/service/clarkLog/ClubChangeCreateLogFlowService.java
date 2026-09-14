package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ClubChangeCreateLogFlow;
import core.db.entity.clarkLog.ClubMemberRemoveLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

/**
 * 颁奖记录 service层
 */
@Service(source = "clark_log")
public class ClubChangeCreateLogFlowService implements BaseService<ClubChangeCreateLogFlow> {
    private BaseClarkLogDao<ClubChangeCreateLogFlow> clarkLogDao = new BaseClarkLogDao<>(ClubChangeCreateLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clarkLogDao;
    }


}




