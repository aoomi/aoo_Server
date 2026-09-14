package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.MaintainGameBO;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import jsproto.c2s.cclass.QueryIdItem;

import java.util.Objects;

@Service(source = "clark_game")
public class MaintainGameBOService implements BaseService<MaintainGameBO> {
    private BaseClarkGameDao<MaintainGameBO> maintainGameBODao = new BaseClarkGameDao<>(MaintainGameBO.class);

    @Override
    public CustomerDao<MaintainGameBO> getDefaultDao() {
        return maintainGameBODao;
    }


    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    @Override
    public long saveOrUpDate(MaintainGameBO element) {
        QueryIdItem idItem = findOneE(Restrictions.eq("gameTypeId", element.getGameTypeId()), QueryIdItem.class, QueryIdItem.getItemsName());
        if (Objects.nonNull(idItem)) {
            element.setId(idItem.getId());
            return update(element);
        }
        return save(element);
    }

    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    @Override
    public long saveIgnoreOrUpDate(MaintainGameBO element) {
        return getDefaultDao().saveOrUpDate(element, true);
    }

}

