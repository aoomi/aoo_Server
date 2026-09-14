package core.dispatch.event.union;

import business.global.club.ClubMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 赛事通知所有管理员
 */
@Data
public class ClubNotify2AllByManageEvent implements BaseExecutor {
    /**
     * 赛事Id
     */
    private long clubId;
    /**
     * 通知信息
     */
    private BaseSendMsg baseSendMsg;

    public ClubNotify2AllByManageEvent(long clubId, BaseSendMsg baseSendMsg) {
        this.setClubId(clubId);
        this.setBaseSendMsg(baseSendMsg);
    }


    @Override
    public void invoke() {
        ClubMgr.getInstance().getClubMemberMgr().notify2AllMinisterByClub(getClubId(), getBaseSendMsg());
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.CLUB_UNION.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.CLUB_UNION.bufferSize();
    }
}
