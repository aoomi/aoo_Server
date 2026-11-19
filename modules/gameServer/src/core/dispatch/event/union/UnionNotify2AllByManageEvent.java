package core.dispatch.event.union;

import business.global.union.UnionMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 赛事通知所有管理员
 */
@Data
public class UnionNotify2AllByManageEvent implements BaseExecutor {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 通知信息
     */
    private BaseSendMsg baseSendMsg;

    public UnionNotify2AllByManageEvent(long unionId, BaseSendMsg baseSendMsg) {
        this.setUnionId(unionId);
        this.setBaseSendMsg(baseSendMsg);
    }


    @Override
    public void invoke() {
        UnionMgr.getInstance().getUnionMemberMgr().notify2AllByManager(getUnionId(), getBaseSendMsg());

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
