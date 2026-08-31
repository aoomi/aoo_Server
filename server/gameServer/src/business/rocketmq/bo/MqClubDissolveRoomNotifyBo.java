package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/4 11:30
 * @description 亲友圈解散房间
 */
@Data
public class MqClubDissolveRoomNotifyBo extends MqAbsBo {
    private long clubId;        //俱乐部ID
    private String roomKey;
    /**
     * 操作人Id
     */
    private long pid;
    /**
     * 操作人名字
     */
    private String playerName;

    public MqClubDissolveRoomNotifyBo(long clubId, String roomKey, long pid, String playerName) {
        this.clubId = clubId;
        this.roomKey = roomKey;
        this.pid = pid;
        this.playerName = playerName;
    }
}
