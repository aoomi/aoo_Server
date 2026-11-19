package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ClubBanPlayerBO;
import core.db.entity.clarkGame.UnionBanPlayerBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import jsproto.c2s.cclass.QueryIdItem;

import java.util.Objects;

@Service(source = "clark_game")
public class ClubBanPlayerBOService implements BaseService<ClubBanPlayerBO> {
    private BaseClarkGameDao<ClubBanPlayerBO> unionBanPlayerBODao = new BaseClarkGameDao<>(ClubBanPlayerBO.class);
    @Override
    public CustomerDao getDefaultDao() {
        return unionBanPlayerBODao;
    }

    /**
     * 存在数据
     *
     * @param criteria
     * @return
     */
    public boolean anyFind(Criteria criteria) {
        return Objects.nonNull(findOneE(criteria, QueryIdItem.class, QueryIdItem.getItemsName()));
    }

    /**
     * 插入
     * @param sql 操作语句
     * @param obj 操作值
     * @return
     */
    @Override
    public Long insert(String sql, Object... obj) {
        return getDefaultDao().insert(sql,obj);
    }


    /**
     * 检查数据是否存在
     * @param criteria
     * @return
     */
    public boolean existFindOneE(Criteria criteria) {
        return Objects.nonNull(findOneE(criteria,QueryIdItem.class,QueryIdItem.getItemsName()));
    }

    /**
     * 检查数据是否存在
     * @param criteria
     * @return
     */
    public boolean notExistFindOneE(Criteria criteria) {
        return !existFindOneE(criteria);
    }

}
