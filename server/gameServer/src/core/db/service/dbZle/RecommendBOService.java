package core.db.service.dbZle;

import com.ddm.server.annotation.Service;
import core.db.dao.dbZle.BaseDbZleDao;
import core.db.entity.dbZle.RecommendBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "db_zle")
public class RecommendBOService implements BaseService<RecommendBO> {
    private BaseDbZleDao<RecommendBO> recommendBODao = new BaseDbZleDao<>(RecommendBO.class);

    @Override
    public CustomerDao<RecommendBO> getDefaultDao() {
        return recommendBODao;
    }
}
