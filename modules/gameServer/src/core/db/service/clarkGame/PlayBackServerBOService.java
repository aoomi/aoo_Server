package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayBackServerBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayBackServerBOService implements BaseService<PlayBackServerBO> {
    private BaseClarkGameDao<PlayBackServerBO> playerPlayBackBODao = new BaseClarkGameDao<>(PlayBackServerBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerPlayBackBODao;
    }
}
