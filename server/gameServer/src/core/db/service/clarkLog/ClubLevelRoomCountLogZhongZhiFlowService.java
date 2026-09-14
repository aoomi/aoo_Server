package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ClubLevelRoomCountLogFlow;
import core.db.entity.clarkLog.ClubLevelRoomCountLogZhongZhiFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class ClubLevelRoomCountLogZhongZhiFlowService implements BaseService<ClubLevelRoomCountLogFlow> {
    private BaseClarkLogDao<ClubLevelRoomCountLogZhongZhiFlow> clubLevelRoomLogFlowDao = new BaseClarkLogDao<>(ClubLevelRoomCountLogZhongZhiFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubLevelRoomLogFlowDao;
    }


}




