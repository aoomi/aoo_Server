package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/3/29 11:30
 * @description 新节点启动通知亲友圈所有成员本地更新
 */
@Data
public class MqClubMemberAllUpdateNotifyBo extends MqAbsBo {
    private String nodeName;        //旧节点名称

    public MqClubMemberAllUpdateNotifyBo(String nodeName) {
        this.nodeName = nodeName;
    }
}
