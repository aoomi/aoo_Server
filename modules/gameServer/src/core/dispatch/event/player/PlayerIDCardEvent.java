package core.dispatch.event.player;

import BaseCommon.CommLog;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.ddm.server.http.client.HttpAsyncClient;
import com.ddm.server.http.client.IResponseHandler;
import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class PlayerIDCardEvent implements BaseExecutor{
    private long pid;
    private int startTime;
    private int endTime;


    public PlayerIDCardEvent(long pid, int startTime, int endTime) {
        this.setPid(pid);
        this.setStartTime(startTime);
        this.setEndTime(endTime);
    }

    @Override
    public void invoke() {

    }


    @Override
    public int threadId() {
        return DispatcherComponentEnum.ID_CARD.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.ID_CARD.bufferSize();
    }
}
