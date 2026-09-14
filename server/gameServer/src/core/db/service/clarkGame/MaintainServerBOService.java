package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.MaintainServerBO;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import jsproto.c2s.cclass.QueryIdItem;

import java.util.Objects;

@Service(source = "clark_game")
public class MaintainServerBOService implements BaseService<MaintainServerBO> {
    private BaseClarkGameDao<MaintainServerBO> maintainServerBODao = new BaseClarkGameDao<>(MaintainServerBO.class);

    @Override
    public CustomerDao<MaintainServerBO> getDefaultDao() {
        return maintainServerBODao;
    }


    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    @Override
    public long saveOrUpDate(MaintainServerBO element) {
        QueryIdItem idItem = findOneE(Restrictions.eq("serverId", element.getServerId()), QueryIdItem.class, QueryIdItem.getItemsName());
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
    public long saveIgnoreOrUpDate(MaintainServerBO element) {
        return getDefaultDao().saveOrUpDate(element, true);
    }

}

