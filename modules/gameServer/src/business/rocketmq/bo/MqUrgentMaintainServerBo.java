package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/10/30 16:30
 * @description 紧急维护对象
 */
@Data
public class MqUrgentMaintainServerBo extends MqAbsBo {
    private String nodeIp;        //节点ip
    private int nodePort;   //节点端口

    public MqUrgentMaintainServerBo(String nodeIp, int nodePort) {
        this.nodeIp = nodeIp;
        this.nodePort = nodePort;
    }

    public MqUrgentMaintainServerBo(String nodeIp) {
        this.nodeIp = nodeIp;
    }
}
