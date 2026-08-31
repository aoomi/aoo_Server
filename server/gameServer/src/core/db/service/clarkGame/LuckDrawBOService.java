package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.LuckDrawBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class LuckDrawBOService implements BaseService<LuckDrawBO> {
    private BaseClarkGameDao<LuckDrawBO> LuckDrawBODao = new BaseClarkGameDao<>(LuckDrawBO.class);

    @Override
    public CustomerDao<LuckDrawBO> getDefaultDao() {
        return LuckDrawBODao;
    }
}

