package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.RoomConfigPrizePoolLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class RoomConfigPrizePoolLogFlowService implements BaseService<RoomConfigPrizePoolLogFlow> {
    private BaseClarkLogDao<RoomConfigPrizePoolLogFlow> roomConfigPrizePoolLogFlowDao = new BaseClarkLogDao<>(RoomConfigPrizePoolLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return roomConfigPrizePoolLogFlowDao;
    }


}

