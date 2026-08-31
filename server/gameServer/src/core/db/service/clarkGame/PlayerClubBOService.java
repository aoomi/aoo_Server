package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerClubBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerClubBOService implements BaseService<PlayerClubBO> {
    private BaseClarkGameDao<PlayerClubBO> playerPlayBackBODao = new BaseClarkGameDao<>(PlayerClubBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerPlayBackBODao;
    }
}
