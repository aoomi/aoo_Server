package core.dispatch.event.other;

import business.global.club.ClubMgr;
import business.global.union.UnionMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;


@Data
public class PromotionActiveReportFormEvent implements BaseExecutor {
    /**
     * 日期时间
     */
    private String dateTime;
    /**
     * 是否00:00点
     */
    private boolean is0Clock;

    public PromotionActiveReportFormEvent(boolean is0Clock) {
        this.is0Clock = is0Clock;
        this.setDateTime(CommTime.getNowTimeStringYMD());
    }

    @Override
    public void invoke() {
        if (is0Clock) {
            UnionMgr.getInstance().getUnionListMgr().unionRoomConfigPrizePoolLog(getDateTime());
        } else {
            ClubMgr.getInstance().getClubMemberMgr().execPromotionLevel(getDateTime());
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.OTHER.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.OTHER.bufferSize();
    }
}
