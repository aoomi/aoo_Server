package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ClubPlayerRemarkNameBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import jsproto.c2s.cclass.QueryIdItem;

import java.util.Objects;

@Service(source = "clark_game")
public class ClubPlayerRemarkNameBOService implements BaseService<ClubPlayerRemarkNameBO> {
    private BaseClarkGameDao<ClubPlayerRemarkNameBO> baseClarkGameDao = new BaseClarkGameDao<>(ClubPlayerRemarkNameBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return baseClarkGameDao;
    }

    /**
     * 插入
     *
     * @param sql 操作语句
     * @param obj 操作值
     * @return
     */
    @Override
    public Long insert(String sql, Object... obj) {
        return getDefaultDao().insert(sql, obj);
    }


    /**
     * 检查数据是否存在
     *
     * @param criteria
     * @return
     */
    public boolean existFindOneE(Criteria criteria) {
        return Objects.nonNull(findOneE(criteria, QueryIdItem.class, QueryIdItem.getItemsName()));
    }

    /**
     * 检查数据是否存在
     *
     * @param criteria
     * @return
     */
    public boolean notExistFindOneE(Criteria criteria) {
        return !existFindOneE(criteria);
    }

}
