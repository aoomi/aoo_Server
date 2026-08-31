package core.dispatch.event.union;

import business.global.club.ClubMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.List;

/**
 * 赛事通知所有参加该赛事的亲友圈成员
 */
@Data
public class UnionNotify2AllByClubEvent implements BaseExecutor {
    /**
     * 获取亲友圈id列表
     */
    private List<Long> clubIdList;
    /**
     * 赛事游戏配置id
     */
    private long unionGameCfgId;
    /**
     * 通知信息
     */
    private BaseSendMsg baseSendMsg;
    /**
     * 存在赛事游戏配置Id T:存在F:不存在
     */
    private boolean existUnionGameCfgId = false;

    public UnionNotify2AllByClubEvent(List<Long> clubIdList, long unionGameCfgId, BaseSendMsg baseSendMsg) {
        this.setClubIdList(clubIdList);
        this.setUnionGameCfgId(unionGameCfgId);
        this.setBaseSendMsg(baseSendMsg);
        this.setExistUnionGameCfgId(true);
    }

    public UnionNotify2AllByClubEvent(List<Long> clubIdList, BaseSendMsg baseSendMsg) {
        this.setClubIdList(clubIdList);
        this.setUnionGameCfgId(unionGameCfgId);
        this.setBaseSendMsg(baseSendMsg);
        this.setExistUnionGameCfgId(false);

    }

    @Override
    public void invoke() {
        if (isExistUnionGameCfgId()) {
            ClubMgr.getInstance().getClubMemberMgr().notify2AllByClub(getClubIdList(), getUnionGameCfgId(), getBaseSendMsg());
        } else {
            ClubMgr.getInstance().getClubMemberMgr().notify2AllByClub(getClubIdList(), getBaseSendMsg());
        }

    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.CLUB_UNION.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.CLUB_UNION.bufferSize();
    }
}
