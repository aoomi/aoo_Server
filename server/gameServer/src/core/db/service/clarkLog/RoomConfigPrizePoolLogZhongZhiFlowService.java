package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.RoomConfigPrizePoolLogZhongZhiFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class RoomConfigPrizePoolLogZhongZhiFlowService implements BaseService<RoomConfigPrizePoolLogZhongZhiFlow> {
    private BaseClarkLogDao<RoomConfigPrizePoolLogZhongZhiFlow> roomConfigPrizePoolLogFlowDao = new BaseClarkLogDao<>(RoomConfigPrizePoolLogZhongZhiFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return roomConfigPrizePoolLogFlowDao;
    }


}

