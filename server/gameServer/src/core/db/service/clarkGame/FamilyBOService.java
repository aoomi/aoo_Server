package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.FamilyBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class FamilyBOService implements BaseService<FamilyBO> {
    private BaseClarkGameDao<FamilyBO> familyBODao = new BaseClarkGameDao<>(FamilyBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return familyBODao;
    }
}

