package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ClubAwardDetailZhongZhiFlow;
import core.db.entity.clarkLog.ClubMemberRemoveLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

/**
 * 颁奖记录 service层
 */
@Service(source = "clark_log")
public class ClubMemberRemoveLogFlowService implements BaseService<ClubMemberRemoveLogFlow> {
    private BaseClarkLogDao<ClubMemberRemoveLogFlow> clarkLogDao = new BaseClarkLogDao<>(ClubMemberRemoveLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clarkLogDao;
    }


}




