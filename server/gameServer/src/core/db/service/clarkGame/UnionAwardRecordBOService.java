package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.UnionAwardRecordBO;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class UnionAwardRecordBOService implements BaseService<UnionAwardRecordBO> {
    private BaseClarkGameDao<UnionAwardRecordBO> clarkGameDao = new BaseClarkGameDao<>(UnionAwardRecordBO.class);
    @Override
    public CustomerDao getDefaultDao() {
        return clarkGameDao;
    }
}
