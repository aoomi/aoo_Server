package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerGameRoomIdBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

@Service(source = "clark_game")
public class PlayerGameRoomIdBOService implements BaseService<PlayerGameRoomIdBO> {
    private BaseClarkGameDao<PlayerGameRoomIdBO> playerGameRoomIdBOBaseClarkGameDao = new BaseClarkGameDao<>(PlayerGameRoomIdBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return playerGameRoomIdBOBaseClarkGameDao;
    }

    /**
     * 如果是用主键primary或者唯一索引unique区分了记录的唯一性,避免重复插入记录可以使用：
     * 保存操作
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    @Override
    public long saveIgnore(PlayerGameRoomIdBO element) {
        return getDefaultDao().add(element, true);
    }

    /**
     * 删除(同步)
     *
     * @param criteria 查询策略
     * @return
     */
    @Override
    public Integer delete(Criteria criteria) {
        return getDefaultDao().delete(criteria);
    }

}

