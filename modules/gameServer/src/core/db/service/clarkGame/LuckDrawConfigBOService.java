package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.LuckDrawConfigBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class LuckDrawConfigBOService implements BaseService<LuckDrawConfigBO> {
    private BaseClarkGameDao<LuckDrawConfigBO> luckDrawConfigBODao = new BaseClarkGameDao<>(LuckDrawConfigBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return luckDrawConfigBODao;
    }
}
