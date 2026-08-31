package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/10/30 16:30
 * @description 维护中
 */
@Data
public class MqDoMaintainServerBo extends MqAbsBo {
    private int maintainServerInt;

    public MqDoMaintainServerBo(int maintainServerInt) {
        this.maintainServerInt = maintainServerInt;
    }
}
