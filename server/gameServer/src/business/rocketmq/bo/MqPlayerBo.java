package business.rocketmq.bo;

import business.shareplayer.SharePlayer;
import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/08/11 16:30
 * @description 玩家信息
 */
@Data
public class MqPlayerBo extends MqAbsBo {
    private SharePlayer sharePlayer;        //玩家信息

    public MqPlayerBo(SharePlayer sharePlayer) {
        this.sharePlayer = sharePlayer;
    }
}
