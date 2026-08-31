package core.dispatch.event.union;

import business.global.club.ClubMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 赛事通知指定亲友圈所有成员
 */
@Data
public class UnionNotify2ClubAllMember implements BaseExecutor {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 通知信息
     */
    private BaseSendMsg baseSendMsg;
    private long playerId;

    public UnionNotify2ClubAllMember(long clubId, long pid, BaseSendMsg baseSendMsg) {
        this.setClubId(clubId);
        this.setBaseSendMsg(baseSendMsg);
        this.setPlayerId(pid);
    }


    @Override
    public void invoke() {
        ClubMgr.getInstance().getClubMemberMgr().unionNotify2ClubAllMember(getClubId(), getPlayerId(), getBaseSendMsg());
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
