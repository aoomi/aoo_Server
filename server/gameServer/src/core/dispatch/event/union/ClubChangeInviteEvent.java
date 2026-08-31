package core.dispatch.event.union;

import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.dispatcher.executor.BaseExecutor;

import java.util.Map;

public class ClubChangeInviteEvent implements BaseExecutor {
    @Override
    public void invoke() {
        Map<Long, ClubMember> allClubMemberCahe = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap();
        for (Map.Entry<Long, ClubMember> con : allClubMemberCahe.entrySet()) {
            con.getValue().getClubMemberBO().saveInvite(1);
        }
        CommLogD.error("=====ClubChangeInviteEvent 执行完毕===：{}");
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.CLUB_INVITE.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.CLUB_INVITE.bufferSize();
    }
}
