package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/13 17:30
 * @description 请求mq通用数据
 */
@Data
public class MqRoomInvitationOperationNotifyBo extends MqAbsBo {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 玩家pid
     */
    private long pid;
    /**
     * 房间id
     */
    private long roomID;

    /**
     * 房间号
     */
    private String roomKey;
}
