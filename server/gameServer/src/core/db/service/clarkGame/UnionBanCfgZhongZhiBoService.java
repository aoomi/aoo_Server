package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.UnionBanCfgZhongZhiBO;
import core.db.entity.clarkGame.UnionGroupingBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class UnionBanCfgZhongZhiBoService implements BaseService<UnionBanCfgZhongZhiBO> {
    private BaseClarkGameDao<UnionBanCfgZhongZhiBO> clarkGameDao = new BaseClarkGameDao<>(UnionBanCfgZhongZhiBO.class);
    @Override
    public CustomerDao getDefaultDao() {
        return clarkGameDao;
    }
}

