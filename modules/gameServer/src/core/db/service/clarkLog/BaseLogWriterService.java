package core.db.service.clarkLog;

import com.ddm.server.annotation.Autowired;
import com.ddm.server.annotation.Service;
import core.db.dao.clarkLog.BaseClarkLogDao;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_log")
public class BaseLogWriterService implements BaseService<Object> {
    @Autowired
    private BaseClarkLogDao<Object> baseClarkLogDao = new BaseClarkLogDao<>(Object.class);

    @Override
    public CustomerDao<Object> getDefaultDao() {
        return baseClarkLogDao;
    }
}
