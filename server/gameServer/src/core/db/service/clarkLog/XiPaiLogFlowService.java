package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.XiPaiLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class XiPaiLogFlowService implements BaseService<XiPaiLogFlow> {
    private BaseClarkLogDao<XiPaiLogFlow> xiPaiLogFlowBaseClarkLogDao = new BaseClarkLogDao<>(XiPaiLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return xiPaiLogFlowBaseClarkLogDao;
    }


}




