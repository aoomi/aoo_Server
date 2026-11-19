package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ClubFatigueDynamicBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class ClubFatigueDynamicBOService implements BaseService<ClubFatigueDynamicBO> {
    private BaseClarkGameDao<ClubFatigueDynamicBO> clubFatigueDynamicBODao = new BaseClarkGameDao<>(ClubFatigueDynamicBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubFatigueDynamicBODao;
    }
}
