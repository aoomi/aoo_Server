package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/09/15 11:30
 * @description 亲友圈赛事解散房间
 */
@Data
public class MqUnionDissolveInitRoomBo extends MqAbsBo {
    private long clubId;        //俱乐部ID
    private long unionId;


    public MqUnionDissolveInitRoomBo(long clubId, long unionId) {
        this.clubId = clubId;
        this.unionId = unionId;
    }
}
