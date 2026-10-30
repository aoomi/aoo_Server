package com.ddm.server.common.rocketmq;

import lombok.Data;

import java.io.Serializable;

/**
 * @author xsj
 * @date 2020/8/17 10:05
 * @description Mq消息的的基类
 */
@Data
public class MqAbsBo implements Serializable {
    protected String clazzName;
}
