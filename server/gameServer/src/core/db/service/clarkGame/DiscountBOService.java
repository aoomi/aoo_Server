package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.DiscountBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class DiscountBOService implements BaseService<DiscountBO> {
    private BaseClarkGameDao<DiscountBO> discountBODao = new BaseClarkGameDao<>(DiscountBO.class);

    @Override
    public CustomerDao<DiscountBO> getDefaultDao() {
        return discountBODao;
    }
}


