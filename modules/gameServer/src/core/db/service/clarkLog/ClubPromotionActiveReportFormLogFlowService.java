package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ClubPromotionActiveReportFormLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class ClubPromotionActiveReportFormLogFlowService implements BaseService<ClubPromotionActiveReportFormLogFlow> {
    private BaseClarkLogDao<ClubPromotionActiveReportFormLogFlow> clubPromotionActiveReportFormLogFlowDao = new BaseClarkLogDao<>(ClubPromotionActiveReportFormLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubPromotionActiveReportFormLogFlowDao;
    }


}

