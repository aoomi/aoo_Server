package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/24 11:30
 * @description 请求赛事mq通用数据
 */
@Data
public class MqPLayerCreateNotifyBo extends MqAbsBo {
    //玩家ID
    private Long pid;


}
