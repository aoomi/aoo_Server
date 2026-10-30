package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ActivityItemBO;
import core.db.entity.clarkGame.ClubMemberPowerZhongZhiBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class ClubMemberPowerZhongZhiBOService implements BaseService<ClubMemberPowerZhongZhiBO> {
    private BaseClarkGameDao<ClubMemberPowerZhongZhiBO> clarkGameDao = new BaseClarkGameDao<>(ClubMemberPowerZhongZhiBO.class);
    @Override
    public CustomerDao<ClubMemberPowerZhongZhiBO> getDefaultDao() {
        return clarkGameDao;
    }
}


