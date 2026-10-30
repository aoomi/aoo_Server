package business.rocketmq.bo;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/13 17:30
 * @description 请求mq通用数据
 */
@Data
public class MqResponseBo extends BaseSendMsg {
    //操作编码
    private String opcode;
    //结果
    private MqDataResult result;
}
