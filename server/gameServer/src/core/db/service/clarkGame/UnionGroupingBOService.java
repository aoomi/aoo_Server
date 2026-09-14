package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.UnionGroupingBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class UnionGroupingBOService implements BaseService<UnionGroupingBO> {
    private BaseClarkGameDao<UnionGroupingBO> unionGroupingBODao = new BaseClarkGameDao<>(UnionGroupingBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return unionGroupingBODao;
    }
}

