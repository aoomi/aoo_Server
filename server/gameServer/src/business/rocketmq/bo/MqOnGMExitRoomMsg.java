package business.rocketmq.bo;

import business.shareplayer.ShareNode;
import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/11/27 14:52
 * @description mq异步消息对象
 */
@Data
public class MqOnGMExitRoomMsg extends MqAbsBo {
    private Long pid;
    //房间节点
    private ShareNode shareNode;
}
