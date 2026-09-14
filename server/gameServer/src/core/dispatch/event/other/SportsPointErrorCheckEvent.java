package core.dispatch.event.other;

import business.global.union.UnionMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;

/**
 * 比赛分总分检查
 */
@Data
public class SportsPointErrorCheckEvent implements BaseExecutor {

    public SportsPointErrorCheckEvent() {
    }

    @Override
    public void invoke() {
        UnionMgr.getInstance().checkSportsPointErrorLog();
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
