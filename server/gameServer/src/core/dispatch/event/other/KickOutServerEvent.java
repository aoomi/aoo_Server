package core.dispatch.event.other;

import business.global.GM.MaintainServerMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;


@Data
public class KickOutServerEvent implements BaseExecutor {

    @Override
    public void invoke() {
        MaintainServerMgr.getInstance().kickOutServer();
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
