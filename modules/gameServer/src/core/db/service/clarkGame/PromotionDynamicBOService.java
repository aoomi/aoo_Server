package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PromotionDynamicBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PromotionDynamicBOService implements BaseService<PromotionDynamicBO> {
    private BaseClarkGameDao<PromotionDynamicBO> unionDynamicBODao = new BaseClarkGameDao<>(PromotionDynamicBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return unionDynamicBODao;
    }
}
