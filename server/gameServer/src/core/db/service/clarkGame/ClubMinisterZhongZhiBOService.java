package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import com.google.common.collect.Lists;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.ClubBanRoomConfigBO;
import core.db.entity.clarkGame.ClubMinisterZhongZhiBO;
import core.db.entity.clarkGame.UnionBanRoomConfigBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import jsproto.c2s.cclass.QueryIdItem;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Service(source = "clark_game")
public class ClubMinisterZhongZhiBOService implements BaseService<ClubMinisterZhongZhiBO> {
    private BaseClarkGameDao<ClubMinisterZhongZhiBO> clarkGameDao = new BaseClarkGameDao<>(ClubMinisterZhongZhiBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return clarkGameDao;
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

    public void execBatchDb(List<ClubMinisterZhongZhiBO> clubMinisterZhongZhiBOS) {
        //新new对象做操作
        List<ClubMinisterZhongZhiBO> list = Lists.newArrayList(clubMinisterZhongZhiBOS);
        if (CollectionUtils.isNotEmpty(list)) {
            int length = list.get(0).addToBatch().length;
            String insetSql = list.get(0).getInsertSql();
            Object[][] parmas = new Object[list.size()][length];
            for (int i = 0; i < list.size(); i++) {
                ClubMinisterZhongZhiBO entity = list.get(i);
                parmas[i] = entity.addToBatch();
            }
            if (list.get(0).getBaseService() != null) {
                list.get(0).getBaseService().batch(insetSql, parmas);
            }
        }
    }
}
