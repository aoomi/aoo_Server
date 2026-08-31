package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/10/30 16:30
 * @description 设置维护时间
 */
@Data
public class MqSetMaintainServerBo extends MqAbsBo {
    private int startTime;        //开始时间
    private int endTime;   //结束时间

    public MqSetMaintainServerBo(int startTime, int endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
