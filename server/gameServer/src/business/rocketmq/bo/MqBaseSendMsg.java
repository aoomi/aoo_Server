package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/14 14:52
 * @description mq异步消息对象
 */
@Data
public class MqBaseSendMsg extends MqAbsBo {
    private String opName;
    private Object body;
}
