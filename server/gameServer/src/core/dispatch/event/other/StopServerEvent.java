package core.dispatch.event.other;

import business.global.GM.MaintainServerMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;


@Data
public class StopServerEvent implements BaseExecutor {

    @Override
    public void invoke() {
        MaintainServerMgr.getInstance().shutdownHook();
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.MAINTAIN.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.MAINTAIN.bufferSize();
    }
}
