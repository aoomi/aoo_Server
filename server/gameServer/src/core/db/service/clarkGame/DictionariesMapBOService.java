package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.DictionariesMapBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class DictionariesMapBOService implements BaseService<DictionariesMapBO> {
    private BaseClarkGameDao<DictionariesMapBO> dictionariesMapBODao = new BaseClarkGameDao<>(DictionariesMapBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return dictionariesMapBODao;
    }
}

