package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerBindingFamilyBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerBindingFamilyBOService implements BaseService<PlayerBindingFamilyBO> {
    private BaseClarkGameDao<PlayerBindingFamilyBO> playerBindingFamilyBODao = new BaseClarkGameDao<>(PlayerBindingFamilyBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerBindingFamilyBODao;
    }
}

