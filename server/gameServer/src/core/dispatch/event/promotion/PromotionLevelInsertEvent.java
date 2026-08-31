package core.dispatch.event.promotion;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.Config;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.entity.clarkGame.ClubMemberRelationBO;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.ioc.ContainerMgr;
import lombok.Data;

import java.util.Arrays;
import java.util.Objects;

/**
 * 代理等级
 */
@Data
public class PromotionLevelInsertEvent implements BaseExecutor {
    /**
     * 亲友圈成员Id
     */
    private long uid;
    /**
     * 上级亲友圈成员Id
     */
    private long puid;

    public PromotionLevelInsertEvent(long uid, long puid) {
        this.setUid(uid);
        this.setPuid(puid);
    }

    @Override
    public void invoke() {
        String sql = String.format("INSERT  IGNORE INTO `ClubMemberRelation`(`uid`, `puid`) SELECT ? AS uid,puid FROM ClubMemberRelation WHERE uid = ?", getUid(), getPuid());
        ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).insert(sql, Arrays.asList(getUid(), getPuid()).toArray(new Object[2]));
        ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).saveIgnore(new ClubMemberRelationBO(getUid(), getPuid()));
        //如果添加的成员 需要进行区间信息初始化的话
        ClubMember clubMember;
        if (Config.isShare()) {
            clubMember = ShareClubMemberMgr.getInstance().getClubMember(uid);
        } else {
            clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(uid);
        }
        if (Objects.isNull(clubMember)) {
            return;
        }
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubMember.getClubID());
        if (Objects.isNull(club)) {
            return;
        }
        ClubMember clubCreateMember = ClubMgr.getInstance().getClubMemberMgr().findCreate(clubMember.getClubID());
        if (Objects.isNull(clubMember)) {
            return;
        }
        if (clubCreateMember.isSectionShare()) {
            clubMember.initPromotionSection(club.getClubListBO().getUnionId(), false);
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.PROMOTION_LEVEL.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PROMOTION_LEVEL.bufferSize();
    }
}
