package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.TaskConfigBO;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class TaskConfigBOService implements BaseService<TaskConfigBO> {
    private BaseClarkGameDao<TaskConfigBO> taskConfigBODao = new BaseClarkGameDao<>(TaskConfigBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return taskConfigBODao;
    }
}


