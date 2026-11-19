package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ClubMemberLatelyConfigIdBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class ClubMemberLatylyConfigIDBOService implements BaseService<ClubMemberLatelyConfigIdBO> {
    private BaseClarkGameDao<ClubMemberLatelyConfigIdBO> discountBODao = new BaseClarkGameDao<>(ClubMemberLatelyConfigIdBO.class);

    @Override
    public CustomerDao<ClubMemberLatelyConfigIdBO> getDefaultDao() {
        return discountBODao;
    }
}


