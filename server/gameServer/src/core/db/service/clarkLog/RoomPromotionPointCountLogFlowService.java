package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.RoomPromotionPointCountLogFlow;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class RoomPromotionPointCountLogFlowService implements BaseService<RoomPromotionPointCountLogFlow> {
    private BaseClarkLogDao<RoomPromotionPointCountLogFlow> roomPromotionPointCountLogFlowBaseClarkLogDao = new BaseClarkLogDao<>(RoomPromotionPointCountLogFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return roomPromotionPointCountLogFlowBaseClarkLogDao;
    }


}




