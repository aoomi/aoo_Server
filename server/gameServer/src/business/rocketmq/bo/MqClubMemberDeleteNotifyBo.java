package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/3/29 17:30
 * @description 亲友圈会员信息删除
 */
@Data
public class MqClubMemberDeleteNotifyBo extends MqAbsBo {
    //亲友圈玩家会员Id
    private Long clubMemberBoId;
    //修改的节点名称
    private String nodeName;

    public MqClubMemberDeleteNotifyBo(Long clubMemberBoId, String nodeName) {
        this.clubMemberBoId = clubMemberBoId;
        this.nodeName = nodeName;
    }
}
