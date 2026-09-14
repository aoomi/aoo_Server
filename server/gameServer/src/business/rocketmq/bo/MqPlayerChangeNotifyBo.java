package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/24 11:30
 * @description 玩家信息变化通知
 */
@Data
public class MqPlayerChangeNotifyBo extends MqAbsBo {
    private long pid;        //玩家ID
    //通知消息内容
    private Object baseSendMsg;

    public MqPlayerChangeNotifyBo(long pid, Object baseSendMsg) {
        this.pid = pid;
        this.baseSendMsg = baseSendMsg;
    }
}
