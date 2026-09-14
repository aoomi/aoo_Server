package core.dispatch.event.union;

import cenum.ConstEnum;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.entity.clarkLog.SportsPointChangeZhongZhiLogFlow;
import core.db.other.Restrictions;
import core.db.service.clarkLog.SportsPointChangeZhongZhiLogBeforeDayFlowService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import jsproto.c2s.cclass.club.ClubSportsPointChangeZhongZhiLogItem;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 推广员分成详细统计
 */
@Data
public class SportsPointChangeZhongZhiCountEvent implements BaseExecutor {

    public SportsPointChangeZhongZhiCountEvent() {

    }

    @Override
    public void invoke() {
        // 查询并统计昨天的数据
        List<ClubSportsPointChangeZhongZhiLogItem> items = ContainerMgr.get().getComponent(SportsPointChangeZhongZhiLogBeforeDayFlowService.class).findAllE(Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(1)).groupBy("pid"), ClubSportsPointChangeZhongZhiLogItem.class, ClubSportsPointChangeZhongZhiLogItem.getItemsName());
        if (CollectionUtils.isNotEmpty(items)) {
            items.forEach(k -> {
                //计算这个房间动态变化前后 玩家身上的竞技点 取id最大最小对应的 curRemainder preValue即可
                SportsPointChangeZhongZhiLogFlow roomPromotionPointLogBeforeDayFlowMax = ContainerMgr.get().getComponent(SportsPointChangeZhongZhiLogBeforeDayFlowService.class).findOne(k.getMaxID());
                SportsPointChangeZhongZhiLogFlow roomPromotionPointLogBeforeDayFlowMin = ContainerMgr.get().getComponent(SportsPointChangeZhongZhiLogBeforeDayFlowService.class).findOne(k.getMinID());
                double curRemainder = Objects.isNull(roomPromotionPointLogBeforeDayFlowMax) ? 0.0 : roomPromotionPointLogBeforeDayFlowMax.getCurRemainder();
                double preValue = Objects.isNull(roomPromotionPointLogBeforeDayFlowMin) ? 0.0 : roomPromotionPointLogBeforeDayFlowMin.getPreValue();
                FlowLogger.sportsPointChangeZhongZhiCountLog(k.getPid(), k.getClubId(), k.getUnionId(), k.getReason(), k.getNum(), curRemainder, preValue, k.getNum() > 0 ? ConstEnum.ResOpType.Gain.ordinal() : ConstEnum.ResOpType.Lose.ordinal(), k.getGameId(), k.getClassificationRegionId(), k.getRoomId());
            });
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.PROMOTION_POINT_RECORD.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PROMOTION_POINT_RECORD.bufferSize();
    }
}
