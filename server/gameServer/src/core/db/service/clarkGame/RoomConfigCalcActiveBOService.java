package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import com.ddm.server.common.ehcache.DataConstants;
import com.ddm.server.common.ehcache.EhCacheFactory;
import com.ddm.server.common.ehcache.configuration.DefaultCacheConfiguration;
import com.ddm.server.common.utils.CommTime;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.RoomConfigCalcActiveBO;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.persistence.BaseDao;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.union.UnionScoreDividedIntoValueItem;
import jsproto.c2s.cclass.union.UnionScorePercentItem;

import java.util.Objects;


@Service(source = "clark_game")
public class RoomConfigCalcActiveBOService implements BaseService<RoomConfigCalcActiveBO> {
    private BaseClarkGameDao<RoomConfigCalcActiveBO> roomConfigCalcActiveBOBaseClarkGameDao = new BaseClarkGameDao<>(RoomConfigCalcActiveBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return roomConfigCalcActiveBOBaseClarkGameDao;
    }


    /**
     * 保存或者更新（异步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    @Override
    public long saveIgnoreOrUpDate(RoomConfigCalcActiveBO element) {
        try {
            UnionScorePercentItem idItem = findOneE(Restrictions.and(Restrictions.eq("pid", element.getPid()), Restrictions.eq("unionId", element.getUnionId()), Restrictions.eq("configId", element.getConfigId()), Restrictions.eq("clubId", element.getClubId())), UnionScorePercentItem.class, UnionScorePercentItem.getItemsName());
            if (idItem != null) {
                if (element.getScoreDividedInto() == idItem.getScoreDividedInto()) {
                    return idItem.getId();
                }
                element.setId(idItem.getId());
                element.setUpdateTime(CommTime.nowSecond());
                return update(element, new AsyncInfo(element.getConfigId()));
            }
        } catch (Exception e) {
            BaseDao.stackTrace("saveIgnoreOrUpDate{}", e);
        } finally {
            EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).put(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, element.getPid(), element.getUnionId(), element.getClubId(), element.getConfigId()), new UnionScoreDividedIntoValueItem(element.getScoreDividedInto(), element.getScorePercent()));
        }
        element.setCreateTime(CommTime.nowSecond());
        return saveIgnore(element, new AsyncInfo(element.getConfigId()));

    }

    /**
     * 查询积分比例
     *
     * @return
     */
    public double findScorePercen(long pid, long unionId, long clubId, long configId, double scorePercent) {
        UnionScoreDividedIntoValueItem item = EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).get(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, pid, unionId, clubId, configId), UnionScoreDividedIntoValueItem.class);
        // 检查缓存是否为空
        if (Objects.isNull(item)) {
            // 检查数据库是否有数据
            item = ContainerMgr.get().getComponent(RoomConfigCalcActiveBOService.class).findOneE(Restrictions.and(Restrictions.eq("pid", pid), Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", clubId), Restrictions.eq("configId", configId)), UnionScoreDividedIntoValueItem.class, UnionScoreDividedIntoValueItem.getItemsName());
            if (Objects.isNull(item)) {
                // 返回本身分成
                return scorePercent;
            } else {
                EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).put(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, pid, unionId, clubId, configId), new UnionScoreDividedIntoValueItem(item.getScoreDividedInto(), item.getScorePercent()));
            }
        }
        return item.getScoreDividedInto();
    }

}
