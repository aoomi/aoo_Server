package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/4 11:30
 * @description 联盟踢出玩家
 */
@Data
public class MqClubKickNotifyBo extends MqAbsBo {
    private long clubId;        //俱乐部ID
    private String roomKey;
    private int posIndex;
    /**
     * 操作人Id
     */
    private long pid;
    /**
     * 操作人名字
     */
    private String playerName;

    public MqClubKickNotifyBo(long clubId, String roomKey, int posIndex, long pid, String playerName) {
        this.clubId = clubId;
        this.roomKey = roomKey;
        this.posIndex = posIndex;
        this.pid = pid;
        this.playerName = playerName;
    }
}
