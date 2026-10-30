package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerCurrencyBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerCurrencyBOService implements BaseService<PlayerCurrencyBO> {
    private BaseClarkGameDao<PlayerCurrencyBO> playerCurrencyBODao = new BaseClarkGameDao<>(PlayerCurrencyBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerCurrencyBODao;
    }
}
