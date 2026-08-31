package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerTaskInfoBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerTaskInfoBOService implements BaseService<PlayerTaskInfoBO> {
    private BaseClarkGameDao<PlayerTaskInfoBO> playerTaskInfoBODao = new BaseClarkGameDao<>(PlayerTaskInfoBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerTaskInfoBODao;
    }
}



