package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ClubDynamicBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class ClubDynamicBOService implements BaseService<ClubDynamicBO> {
    private BaseClarkGameDao<ClubDynamicBO> clubDynamicBODao = new BaseClarkGameDao<>(ClubDynamicBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clubDynamicBODao;
    }
}
