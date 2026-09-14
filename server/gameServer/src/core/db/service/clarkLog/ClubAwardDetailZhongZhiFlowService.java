package core.db.service.clarkLog;

import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.annotation.Service;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.google.gson.Gson;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.entity.clarkLog.ClubAwardDetailZhongZhiFlow;
import core.db.entity.clarkLog.ClubLevelRoomAwardLogZhongZhiFlow;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.ClubPromotionLevelItem;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 颁奖记录 service层
 */
@Service(source = "clark_log")
public class ClubAwardDetailZhongZhiFlowService implements BaseService<ClubAwardDetailZhongZhiFlow> {
    private BaseClarkLogDao<ClubAwardDetailZhongZhiFlow> clarkLogDao = new BaseClarkLogDao<>(ClubAwardDetailZhongZhiFlow.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clarkLogDao;
    }


}




