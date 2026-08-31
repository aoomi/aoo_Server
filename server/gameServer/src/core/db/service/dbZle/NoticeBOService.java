package core.db.service.dbZle;

import com.ddm.server.annotation.Service;
import core.db.dao.dbZle.BaseDbZleDao;
import core.db.entity.dbZle.NoticeBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "db_zle")
public class NoticeBOService implements BaseService<NoticeBO> {
    private BaseDbZleDao<NoticeBO> noticeBODao = new BaseDbZleDao<>(NoticeBO.class);

    @Override
    public CustomerDao<NoticeBO> getDefaultDao() {
        return noticeBODao;
    }
}
