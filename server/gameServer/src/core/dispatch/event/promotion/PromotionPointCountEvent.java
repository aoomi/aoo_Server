package core.dispatch.event.promotion;

import cenum.ConstEnum;
import cenum.DispatcherComponentEnum;
import cenum.ItemFlow;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.entity.clarkLog.RoomPromotionPointLogBeforeDayFlow;
import core.db.other.Restrictions;
import core.db.service.clarkLog.RoomPromotionPointLogBeforeDayFlowService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import jsproto.c2s.cclass.club.ClubRoomPromotionPointCountLogItem;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 推广员分成详细统计
 */
@Data
public class PromotionPointCountEvent implements BaseExecutor {

    public PromotionPointCountEvent() {

    }

    @Override
    public void invoke() {
        // 查询并统计昨天的数据
        List<ClubRoomPromotionPointCountLogItem> items = ContainerMgr.get().getComponent(RoomPromotionPointLogBeforeDayFlowService.class).findAllE(Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(1)).groupBy("roomId`,`pid"), ClubRoomPromotionPointCountLogItem.class, ClubRoomPromotionPointCountLogItem.getItemsName());
        if (CollectionUtils.isNotEmpty(items)) {
            items.forEach(k -> {
                //计算这个房间动态变化前后 玩家身上的竞技点 取id最大最小对应的 curRemainder preValue即可
                RoomPromotionPointLogBeforeDayFlow roomPromotionPointLogBeforeDayFlowMax = ContainerMgr.get().getComponent(RoomPromotionPointLogBeforeDayFlowService.class).findOne(k.getMaxID());
                RoomPromotionPointLogBeforeDayFlow roomPromotionPointLogBeforeDayFlowMin = ContainerMgr.get().getComponent(RoomPromotionPointLogBeforeDayFlowService.class).findOne(k.getMinID());
                double curRemainder = Objects.isNull(roomPromotionPointLogBeforeDayFlowMax) ? 0.0 : roomPromotionPointLogBeforeDayFlowMax.getCurRemainder();
                double preValue = Objects.isNull(roomPromotionPointLogBeforeDayFlowMin) ? 0.0 : roomPromotionPointLogBeforeDayFlowMin.getPreValue();
                int execTime = Objects.isNull(roomPromotionPointLogBeforeDayFlowMin) ? 0 : roomPromotionPointLogBeforeDayFlowMin.getTimestamp();
                FlowLogger.roomPromotionPointCountLog(k.getPid(), k.getDateTime(), k.getClubId(), k.getUnionId(), ItemFlow.PROMOTION_SPORTS_POINT_PROFIT_CASEPOINT.value(), k.getNum(), curRemainder, preValue, k.getNum() > 0 ? ConstEnum.ResOpType.Gain.ordinal() : ConstEnum.ResOpType.Lose.ordinal(), k.getGameId(), k.getClassificationRegionId(), k.getRoomId(), 0, k.getRoomName(), "", execTime, k.getRoomKey());
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
