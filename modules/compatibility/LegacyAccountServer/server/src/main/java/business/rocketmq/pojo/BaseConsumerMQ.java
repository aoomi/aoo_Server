package business.rocketmq.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseConsumerMQ implements Serializable {
    /**
     * 是否存在测试
     */
    private boolean existTest = true;

    public BaseConsumerMQ(boolean existTest) {
        this.existTest = existTest;
    }

    public BaseConsumerMQ() {
    }
    public boolean isNotExistTest() {
        return !this.existTest;
    }



}
