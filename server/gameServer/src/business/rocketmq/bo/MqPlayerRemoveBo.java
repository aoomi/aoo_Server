package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/08/11 16:30
 * @description 玩家信息
 */
@Data
public class MqPlayerRemoveBo extends MqAbsBo {
    private Long pid;        //玩家ID

    public MqPlayerRemoveBo(Long pid) {
        this.pid = pid;
    }
}
