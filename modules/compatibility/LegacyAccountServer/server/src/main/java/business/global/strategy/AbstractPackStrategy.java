package business.global.strategy;

import com.ddm.server.annotation.OperLog;

/**
 * 请求包的策略
 * 抽象类，此处也可以使用接口，根据具体情况定义
 */
public abstract class AbstractPackStrategy {

    /**
     * 处理接收到的包数据
     * @param senderInfo 发件人信息
     * @param receivePack 接收包信息
     * @return
     */
    public abstract String OnReceivePack(String senderInfo,String receivePack);
}
