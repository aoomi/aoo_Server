package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ExamineLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

/**
 * 推广员审核功能
 */
@Service(source = "clark_log")
public class ExamineFlogService implements BaseService<ExamineLogFlow> {
    private BaseClarkLogDao<ExamineLogFlow> examineLogFlowBaseClarkLogDao = new BaseClarkLogDao<>(ExamineLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return examineLogFlowBaseClarkLogDao;
    }


}




