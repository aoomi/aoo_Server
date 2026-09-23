package core.logger.flow.disruptor.log;

import core.db.entity.BaseClarkLogEntity;

/**
 * 派发器接口
 */
public interface BatchDbLog {

    void publish(BaseClarkLogEntity executor);

    void init();

    void start();
}