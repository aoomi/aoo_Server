package core.dispatch.event.localcache;

import business.global.club.ClubMember;
import business.global.shareclub.LocalClubMemberMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import lombok.Data;

/**
 * 本地缓存成员修改
 */
@Data
public class LocalClubMemberEvent implements BaseExecutor {
    /**
     * 成员数据
     */
    private ClubMember clubMember;
    private Boolean isAdd;

    public LocalClubMemberEvent(ClubMember clubMember, Boolean isAdd) {
        this.clubMember = clubMember;
        this.isAdd = isAdd;
    }

    @Override
    public void invoke() {
        if (isAdd) {
            LocalClubMemberMgr.getInstance().addClubMember(clubMember);
        } else {
            LocalClubMemberMgr.getInstance().removeClubMember(clubMember);
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.LOCAL_CLUB_MEMBER.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.LOCAL_CLUB_MEMBER.bufferSize();
    }
}
