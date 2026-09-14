package business.rocketmq.bo;

import business.global.shareroom.ShareRoom;
import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/08/11 16:30
 * @description 房间信息
 */
@Data
public class MqRoomBo extends MqAbsBo {
    private ShareRoom shareRoom;        //房间信息

    public MqRoomBo(ShareRoom shareRoom) {
        this.shareRoom = shareRoom;
    }
}
