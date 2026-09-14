package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import com.ddm.server.common.ehcache.DataConstants;
import com.ddm.server.common.ehcache.EhCacheFactory;
import com.ddm.server.common.ehcache.configuration.DefaultCacheConfiguration;
import com.ddm.server.common.utils.CommTime;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PromotionLevelRoomConfigScorePercentBO;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.db.other.AsyncInfo;
import core.db.other.Restrictions;
import core.db.persistence.BaseDao;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.cclass.union.UnionScoreDividedIntoValueItem;
import jsproto.c2s.cclass.union.UnionScorePercentItem;

import java.util.Objects;


@Service(source = "clark_game")
public class PromotionLevelRoomConfigScorePercentBOService implements BaseService<PromotionLevelRoomConfigScorePercentBO> {
    private BaseClarkGameDao<PromotionLevelRoomConfigScorePercentBO> unionRoomConfigScorePercentBOBaseClarkGameDao = new BaseClarkGameDao<>(PromotionLevelRoomConfigScorePercentBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return unionRoomConfigScorePercentBOBaseClarkGameDao;
    }

    /**
     * 保存或者更新（异步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    public long saveIgnoreOrUpDate(PromotionLevelRoomConfigScorePercentBO element, long execPid) {
        try {
            UnionScorePercentItem idItem = findOneE(Restrictions.and(Restrictions.eq("pid", element.getPid()), Restrictions.eq("unionId", element.getUnionId()), Restrictions.eq("configId", element.getConfigId()), Restrictions.eq("clubId", element.getClubId())), UnionScorePercentItem.class, UnionScorePercentItem.getItemsName());
            if (idItem != null) {
                if (element.getType() == idItem.getType()) {
                    if ((element.getType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() && element.getScoreDividedInto() == idItem.getScoreDividedInto()) || (element.getType() == UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal() && element.getScorePercent() == idItem.getScorePercent())) {
                        return idItem.getId();
                    }
                }
                element.setId(idItem.getId());
                element.setUpdateTime(CommTime.nowSecond());
                if (element.getType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
                    // 修改固定值,百分比不变
                    element.setScorePercent(idItem.getScorePercent());
                    UnionDynamicBO.insertRoomSportsPoint(element.getPid(), element.getClubId(), execPid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), element.getUnionId(), String.valueOf(element.getScoreDividedInto()), String.valueOf(idItem.getScoreDividedInto() < 0 ? element.getShareValue() : idItem.getScoreDividedInto()), "", String.format("%s(%d)", element.getConfigName(), element.getTagId()));
                } else {
                    // 修改百分比,固定值不变
                    element.setScoreDividedInto(idItem.getScoreDividedInto());
                    UnionDynamicBO.insertRoomSportsPoint(element.getPid(), element.getClubId(), execPid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), element.getUnionId(), String.valueOf(element.getScorePercent() + "%"), String.valueOf(idItem.getScorePercent() < 0 ? element.getShareValue() : idItem.getScorePercent() + "%"), "", String.format("%s(%d)", element.getConfigName(), element.getTagId()));
                }
                return update(element, new AsyncInfo(element.getConfigId()));
            }
        } catch (Exception e) {
            BaseDao.stackTrace("saveIgnoreOrUpDate{}", e);
        } finally {
            EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).put(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, element.getPid(), element.getUnionId(), element.getClubId(), element.getConfigId()), new UnionScoreDividedIntoValueItem(element.getScoreDividedInto(), element.getScorePercent()));
        }
        if (element.getType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
            // 设置固定值，则百分比默认值要设为 -1
            element.setScorePercent(-1);
        } else {
            // 否则 固定值 设为 -1
            element.setScoreDividedInto(-1);
        }
        element.setCreateTime(CommTime.nowSecond());
        if (element.getScoreDividedInto() != element.getShareValue()) {
            if (element.getType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
                UnionDynamicBO.insertRoomSportsPoint(element.getPid(), element.getClubId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), element.getUnionId(), String.valueOf(element.getScorePercent()), String.valueOf(element.getShareValue()), "", String.format("%s(%d)", element.getConfigName(), element.getTagId()));
            } else {
                UnionDynamicBO.insertRoomSportsPoint(element.getPid(), element.getClubId(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), element.getUnionId(), String.valueOf(element.getScorePercent() + "%"), String.valueOf(element.getShareValue() + "%"), "", String.format("%s(%d)", element.getConfigName(), element.getTagId()));
            }
        }
        return saveIgnore(element, new AsyncInfo(element.getConfigId()));


    }

    /**
     * 查询积分比例
     *
     * @return
     */
    public double findScorePercen(long pid, long unionId, long clubId, long configId, int type, double scorePercent) {
        UnionScoreDividedIntoValueItem item = EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).get(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, pid, unionId, clubId, configId), UnionScoreDividedIntoValueItem.class);
        // 检查缓存是否为空
        if (Objects.isNull(item)) {
            // 检查数据库是否有数据
            item = ContainerMgr.get().getComponent(PromotionLevelRoomConfigScorePercentBOService.class).findOneE(Restrictions.and(Restrictions.eq("pid", pid), Restrictions.eq("unionId", unionId), Restrictions.eq("clubId", clubId), Restrictions.eq("configId", configId)), UnionScoreDividedIntoValueItem.class, UnionScoreDividedIntoValueItem.getItemsName());
            if (Objects.isNull(item)) {
                // 返回本身分成
                return scorePercent;
            } else {
                EhCacheFactory.getCacheApi(DefaultCacheConfiguration.class).put(String.format(DataConstants.PROMOTION_LEVEL_ROOM_CONFIG_SCORE_PERCENT, pid, unionId, clubId, configId), new UnionScoreDividedIntoValueItem(item.getScoreDividedInto(), item.getScorePercent()));
            }
        }
        if (type == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
            return item.getScoreDividedInto() >= 0D ? item.getScoreDividedInto() : scorePercent;
        } else {
            return item.getScorePercent() >= 0D ? item.getScorePercent() : scorePercent;
        }
    }


}
