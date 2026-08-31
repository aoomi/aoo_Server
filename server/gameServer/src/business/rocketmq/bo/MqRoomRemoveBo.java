package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/08/11 16:30
 * @description 房间号
 */
@Data
public class MqRoomRemoveBo extends MqAbsBo {
    private String roomKey;        //房间号

    public MqRoomRemoveBo(String roomKey) {
        this.roomKey = roomKey;
    }
}
