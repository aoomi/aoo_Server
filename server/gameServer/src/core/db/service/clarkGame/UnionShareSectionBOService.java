package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.UnionShareSectionBO;
import core.db.other.Restrictions;
import core.db.persistence.BaseDao;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import jsproto.c2s.cclass.QueryIdItem;

@Service(source = "clark_game")
public class UnionShareSectionBOService implements BaseService<UnionShareSectionBO> {
    private BaseClarkGameDao<UnionShareSectionBO> unionShareSectionBOBaseClarkGameDao = new BaseClarkGameDao<>(UnionShareSectionBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return unionShareSectionBOBaseClarkGameDao;
    }

    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    @Override
    public long saveIgnoreOrUpDate(UnionShareSectionBO element) {
        try {
            QueryIdItem idItem = findOneE(Restrictions.and(Restrictions.eq("unionId", element.getUnionId()), Restrictions.eq("beginValue", element.getBeginValue()), Restrictions.eq("endValue", element.getEndValue())), QueryIdItem.class, QueryIdItem.getItemsName());
            if (idItem != null) {
                element.setId(idItem.getId());
                return update(element);
            }
        } catch (Exception e) {
            BaseDao.stackTrace("saveIgnoreOrUpDate{}", e);
        }
        return saveIgnore(element);
    }
}
