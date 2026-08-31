package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/10/30 16:30
 * @description 踢出游戏
 */
@Data
public class MqKickOutGameBo extends MqAbsBo {
    private String nodeIp;        //节点ip
    private int nodePort;   //节点端口
    private Integer gameTypeId; //游戏Id

    public MqKickOutGameBo(String nodeIp, int nodePort, Integer gameTypeId) {
        this.nodeIp = nodeIp;
        this.nodePort = nodePort;
        this.gameTypeId = gameTypeId;
    }
}
