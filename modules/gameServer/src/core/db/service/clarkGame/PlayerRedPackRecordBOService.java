package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerRedPackRecordBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerRedPackRecordBOService implements BaseService<PlayerRedPackRecordBO> {
    private BaseClarkGameDao<PlayerRedPackRecordBO> playerRedPackRecordBODao = new BaseClarkGameDao<>(PlayerRedPackRecordBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerRedPackRecordBODao;
    }
}

