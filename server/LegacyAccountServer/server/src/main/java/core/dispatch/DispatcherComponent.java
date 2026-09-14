
package core.dispatch;

import cenum.DispatcherComponentEnum;
import cenum.ServerSceneEnum;
import com.ddm.server.dispatcher.disruptor.DisruptorService;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;

import java.util.Arrays;

/**
 * 默认的消息派发中心实现
 */
@Data
public class DispatcherComponent implements Dispatcher {
    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static DispatcherComponent instance = new DispatcherComponent();
    }

    // 私有化构造方法
    private DispatcherComponent() {
    }

    // 获取单例
    public static DispatcherComponent getInstance() {
        return DispatcherComponent.SingletonHolder.instance;
    }

    /**
     * 任务调度服务
     */
    private final DisruptorService disruptorService = new DisruptorService();


    @Override
    public void publish(BaseExecutor executor) {
        this.getDisruptorService().publish(executor);
    }

    @Override
    public void init(ServerSceneEnum serverSceneEnum) {
        Arrays.stream(DispatcherComponentEnum.values()).forEach(k -> {
            if (ServerSceneEnum.ALL.equals(k.getSceneEnum())) {
                this.getDisruptorService().addThread(k.name(), k.id(), k.bufferSize());
            } else if (serverSceneEnum.equals(k.getSceneEnum())) {
                this.getDisruptorService().addThread(k.name(), k.id(), k.bufferSize());
            }
        });
        this.start();
    }

    @Override
    public void start() {
        this.getDisruptorService().start();
    }


}
