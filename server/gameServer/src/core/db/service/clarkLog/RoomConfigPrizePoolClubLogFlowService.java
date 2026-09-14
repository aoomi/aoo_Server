package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.RoomConfigPrizePoolClubLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class RoomConfigPrizePoolClubLogFlowService implements BaseService<RoomConfigPrizePoolClubLogFlow> {
    private BaseClarkLogDao<RoomConfigPrizePoolClubLogFlow> roomConfigPrizePoolLogFlowDao = new BaseClarkLogDao<>(RoomConfigPrizePoolClubLogFlow.class);
    
    @Override
    public CustomerDao getDefaultDao() {
        return roomConfigPrizePoolLogFlowDao;
    }


}

