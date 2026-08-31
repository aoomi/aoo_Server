package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/4 11:30
 * @description 联盟解散房间通知
 */
@Data
public class MqUnionDissolveRoomNotifyBo extends MqAbsBo {
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 房间号
     */
    private String roomKey;
    /**
     * 操作人Id
     */
    private long pid;
    /**
     * 操作人名字
     */
    private String playerName;

    public MqUnionDissolveRoomNotifyBo(long unionId, long clubId, String roomKey, long pid, String playerName) {
        this.unionId = unionId;
        this.clubId = clubId;
        this.roomKey = roomKey;
        this.pid = pid;
        this.playerName = playerName;
    }
}
