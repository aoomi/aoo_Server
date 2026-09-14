package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/3/24 17:30
 * @description 请求房间比赛分变化
 */
@Data
public class MqRoomSportsPointNotifyBo extends MqAbsBo {
    private long pid;        //玩家ID
    /**
     * 值
     */
    private double value;
    /**
     * 成员Id
     */
    private long memberId;
    /**
     * 房间Id
     */
    private long roomId;

    public MqRoomSportsPointNotifyBo(long pid, double value, long memberId, long roomId) {
        this.pid = pid;
        this.value = value;
        this.memberId = memberId;
        this.roomId = roomId;
    }
}
