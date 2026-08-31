package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/4 11:30
 * @description 联盟踢出玩家
 */
@Data
public class MqUnionKickNotifyBo extends MqAbsBo {
    /**
     * 房间key
     */
    private String roomKey;

    /**
     * 位置
     */
    private int posIndex;

    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 操作人Id
     */
    private long pid;
    /**
     * 操作人名字
     */
    private String playerName;

    public MqUnionKickNotifyBo(String roomKey, int posIndex, long unionId, long clubId, long pid, String playerName) {
        this.roomKey = roomKey;
        this.posIndex = posIndex;
        this.unionId = unionId;
        this.clubId = clubId;
        this.pid = pid;
        this.playerName = playerName;
    }
}
