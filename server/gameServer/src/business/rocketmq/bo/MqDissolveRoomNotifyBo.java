package business.rocketmq.bo;

import business.shareplayer.ShareNode;
import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/4 11:30
 * @description 解散房间
 */
@Data
public class MqDissolveRoomNotifyBo extends MqAbsBo {
    private String roomKey;
    //房间节点
    private ShareNode shareNode;

    public MqDissolveRoomNotifyBo(String roomKey, ShareNode shareNode) {
        this.roomKey = roomKey;
        this.shareNode = shareNode;
    }
}
