package business.rocketmq.bo;

import business.global.club.ClubMember;
import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2021/08/09 16:30
 * @description 成员信息
 */
@Data
public class MqClubMemberBo extends MqAbsBo {
    private ClubMember clubMember;        //成员信息

    public MqClubMemberBo(ClubMember clubMember) {
        this.clubMember = clubMember;
    }
}
