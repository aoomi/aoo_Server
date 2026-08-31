package core.dispatch.event.promotion;

import cenum.DispatcherComponentEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.other.Restrictions;
import core.db.service.clarkLog.ClubLevelRoomLogBeforeDayFlowService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import jsproto.c2s.cclass.club.ClubLevelRoomCountLogItem;
import jsproto.c2s.cclass.club.ClubRoomSizeItem;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * 推广员等级统计
 */
@Data
public class PromotionLevelCountEvent implements BaseExecutor {

    public PromotionLevelCountEvent() {

    }

    @Override
    public void invoke() {
        // 查询并统计昨天的数据
        List<ClubLevelRoomCountLogItem> items = ContainerMgr.get().getComponent(ClubLevelRoomLogBeforeDayFlowService.class).findAllE(Restrictions.eq("date_time", CommTime.getYesterDayStringYMD(1)).groupBy("clubId`,`memberId"), ClubLevelRoomCountLogItem.class, ClubLevelRoomCountLogItem.getItemsName());
        if (CollectionUtils.isNotEmpty(items)) {
            items.forEach(k -> FlowLogger.clubLevelRoomCountLog(k.getDate_time(), k.getSetCount(), k.getWinner(), k.getConsume(), 0, k.getUpLevelId(), k.getMemberId(), k.getSumPoint(), k.getSportsPoint(), k.getSportsPointConsume(), k.getRoomSportsPointConsume(), k.getRoomAvgSportsPointConsume(), k.getClubId(), k.getUnionId(), k.getPromotionShareValue()));
        }
        List<ClubRoomSizeItem> roomSizeList = ((ClubLevelRoomLogBeforeDayFlowService) ContainerMgr.get().getComponent(ClubLevelRoomLogBeforeDayFlowService.class)).getRoomSizeList(null, CommTime.getYesterDayStringYMD(1), ClubRoomSizeItem.class, null);
        if (CollectionUtils.isNotEmpty(roomSizeList)) {
            roomSizeList.forEach(item -> FlowLogger.clubLevelRoomCountLog(item.getDateTime(), 0, 0, 0, item.getRoomSize(), 0, 0, 0, 0D, 0D, 0D, 0D, item.getClubId(), 0, 0D));
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.PROMOTION_LEVEL.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PROMOTION_LEVEL.bufferSize();
    }
}
