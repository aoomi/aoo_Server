package core.db.service.dbZle;

import com.ddm.server.annotation.Service;
import core.db.dao.dbZle.BaseDbZleDao;
import core.db.entity.dbZle.ShareTiXian;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "db_zle")
public class ShareTiXianService implements BaseService<ShareTiXian> {
    private BaseDbZleDao<ShareTiXian> shareTiXianDao = new BaseDbZleDao<>(ShareTiXian.class);

    public Long sum(Criteria criteria, String property) {
        return shareTiXianDao.sum(criteria, property);
    }

    @Override
    public CustomerDao<ShareTiXian> getDefaultDao() {
        return shareTiXianDao;
    }
}

