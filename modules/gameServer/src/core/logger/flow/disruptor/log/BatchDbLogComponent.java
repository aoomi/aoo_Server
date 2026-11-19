package core.logger.flow.disruptor.log;

import cenum.DispatcherComponentLogEnum;
import core.db.entity.BaseClarkLogEntity;
import lombok.Data;

import java.util.Arrays;

/**
 * 默认的消息派发中心实现
 */
@Data
public class BatchDbLogComponent implements BatchDbLog {
    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static BatchDbLogComponent instance = new BatchDbLogComponent();
    }

    // 私有化构造方法
    private BatchDbLogComponent() {
    }

    // 获取单例
    public static BatchDbLogComponent getInstance() {
        return BatchDbLogComponent.SingletonHolder.instance;
    }

    /**
     * 任务调度服务
     */
    private final BatchDbLogService disruptorService = new BatchDbLogService();


    @Override
    public void publish(BaseClarkLogEntity executor) {
        this.getDisruptorService().publish(executor);
    }

    @Override
    public void init() {
        Arrays.stream(DispatcherComponentLogEnum.values()).forEach(k -> {
            this.getDisruptorService().addThread(k.name(), k.id(), k.bufferSize());
        });
        this.start();
    }

    @Override
    public void start() {
        this.getDisruptorService().start();
    }


}
