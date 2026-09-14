package com.ddm.server.common.rocketmq;


public interface MqConsumerHandler {
    void action(Object body) throws ClassNotFoundException;
}
