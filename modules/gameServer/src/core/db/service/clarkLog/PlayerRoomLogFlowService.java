package core.db.service.clarkLog;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.PlayerRoomLogFlow;
import core.db.other.AsyncInfo;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class PlayerRoomLogFlowService implements BaseService<PlayerRoomLogFlow> {
    private BaseClarkLogDao<PlayerRoomLogFlow> playerRoomLogFlowDao = new BaseClarkLogDao<>(PlayerRoomLogFlow.class);

    public Long sum(String sumKey, String mothTime, long clubId, int clubCostType, int yearOrMonth, AsyncInfo asyncInfo) {
        Criteria criteria = Restrictions.and(Restrictions.eq("club_id", clubId), Restrictions.eq("clubCostType", clubCostType), Restrictions.eq("LEFT(date_time, " + yearOrMonth + ")", mothTime));
        return playerRoomLogFlowDao.sum(criteria, sumKey);
    }

    @Override
    public CustomerDao getDefaultDao() {
        return playerRoomLogFlowDao;
    }
}

