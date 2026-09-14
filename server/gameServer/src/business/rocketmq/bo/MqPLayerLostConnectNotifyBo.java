package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/7 11:30
 * @description 玩家是否丢掉连接通知
 */
@Data
public class MqPLayerLostConnectNotifyBo extends MqAbsBo {
    //玩家ID
    private Long pid;
    //房间ID
    private Long roomId;
    //是否掉线
    private Boolean isLostConnect;

    public MqPLayerLostConnectNotifyBo(Long pid, Long roomId, Boolean isLostConnect) {
        this.pid = pid;
        this.roomId = roomId;
        this.isLostConnect = isLostConnect;
    }
}
