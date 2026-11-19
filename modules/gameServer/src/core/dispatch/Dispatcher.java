package core.dispatch;

import com.ddm.server.dispatcher.executor.BaseExecutor;

/**
 * 派发器接口
 */
public interface Dispatcher {

    void publish(BaseExecutor executor);

    void init();

    void start();
}