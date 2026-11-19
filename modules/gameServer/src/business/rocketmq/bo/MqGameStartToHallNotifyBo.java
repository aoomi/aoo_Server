package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/11 11:30
 * @description 新节点启动通知大厅
 */
@Data
public class MqGameStartToHallNotifyBo extends MqAbsBo {
    private String nodeName;        //新节点名称

    public MqGameStartToHallNotifyBo(String nodeName) {
        this.nodeName = nodeName;
    }
}
