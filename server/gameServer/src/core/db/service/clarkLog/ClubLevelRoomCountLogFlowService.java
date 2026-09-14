package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ClubLevelRoomCountLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class ClubLevelRoomCountLogFlowService implements BaseService<ClubLevelRoomCountLogFlow> {
    private BaseClarkLogDao<ClubLevelRoomCountLogFlow> clubLevelRoomLogFlowDao = new BaseClarkLogDao<>(ClubLevelRoomCountLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubLevelRoomLogFlowDao;
    }


}




