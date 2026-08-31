package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/9/2 17:30
 * @description 亲友圈会员信息更新
 */
@Data
public class MqClubMemberUpdateNotifyBo extends MqAbsBo {
    //亲友圈玩家会员Id
    private Long clubMemberBoId;
    //修改的节点名称
    private String nodeName;

    public MqClubMemberUpdateNotifyBo(Long clubMemberBoId, String nodeName) {
        this.clubMemberBoId = clubMemberBoId;
        this.nodeName = nodeName;
    }
}
