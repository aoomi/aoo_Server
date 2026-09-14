package core.dispatch.event.player;

import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;


/**
 * 释放玩家
 * @author Administrator
 *
 */
@Data
public class ReleasEvent implements BaseExecutor {

	@Override
	public void invoke() {
	}
	
	@Override
	public int threadId() {
		return DispatcherComponentEnum.PLAYER.id();
	}

	@Override
	public int bufferSize() {
		return DispatcherComponentEnum.PLAYER.bufferSize();
	}
}