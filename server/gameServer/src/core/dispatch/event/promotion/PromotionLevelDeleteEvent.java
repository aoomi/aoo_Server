package core.dispatch.event.promotion;

import BaseCommon.CommLog;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.google.common.collect.Maps;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.db.service.clarkLog.ClubLevelRoomCountLogFlowService;
import core.db.service.clarkLog.ClubLevelRoomLogBeforeDayFlowService;
import core.db.service.clarkLog.ClubLevelRoomLogFlowService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.Club_define;
import lombok.Data;

import java.util.Map;
import java.util.Objects;

/**
 * 代理等级
 */
@Data
public class PromotionLevelDeleteEvent implements BaseExecutor {
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     * 亲友圈成员Id
     */
    private long uid;
    /**
     * 上级亲友圈成员Id
     */
    private long puid;
    /**
     * 是否删除
     */
    private Club_define.Club_PROMOTION_LEVEL_DELETE levelDelete;
    /**
     * 时间
     */
    private String dateTime;

    public PromotionLevelDeleteEvent(long clubId, long uid, long puid, Club_define.Club_PROMOTION_LEVEL_DELETE levelDelete, String dateTime) {
        this.setClubId(clubId);
        this.setUid(uid);
        this.setPuid(puid);
        this.setLevelDelete(levelDelete);
        this.setDateTime(dateTime);
    }

    @Override
    public void invoke() {
        if (getPuid() > 0L) {
            if (Club_define.Club_PROMOTION_LEVEL_DELETE.DELETE.equals(getLevelDelete()) || Club_define.Club_PROMOTION_LEVEL_DELETE.CANCEL_TOP_LEVEL.equals(getLevelDelete())) {
                // 删除推广员或者卸任顶级推广员
                // 删除推广员 uid 绑定数据
                ContainerMgr.get().getComponent(ClubMemberRelationBOService.class).delete(Restrictions.eq("uid", getUid()));
                CommLogD.error("PromotionLevelDeleteEvent delete one uid:" + getUid());
            }
            // 删除上级推广员是 uid 的绑定数据
            ContainerMgr.get().getComponent(ClubMemberRelationBOService.class).delete(Restrictions.eq("puid", getUid()));
            CommLogD.error("PromotionLevelDeleteEvent delete two puid:" + getUid());
            // 更新上级推广员 uid 的绑定改为 绑定uid 的上级
            ClubMgr.getInstance().getClubMemberMgr().updateClubMemberUpLevelId(getClubId(), getUid(), getPuid());
        }
        if (Club_define.Club_PROMOTION_LEVEL_DELETE.DELETE.equals(getLevelDelete())) {
            // 转移到上级
            long newUid = getPuid();
            // 上级的上级
            long newUpid = 0L;
            ClubMember clubMember;
            if (Config.isShare()) {
                clubMember = ShareClubMemberMgr.getInstance().getClubMember(getPuid());
            } else {
                clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(getPuid());
            }

            if (Objects.isNull(clubMember)) {
                // 获取创建者id
                newUid = ClubMgr.getInstance().getClubMemberMgr().getClubCreateMemberId(getClubId());
            } else {
                newUpid = clubMember.getClubMemberBO().getUpLevelId();
            }
            if (newUid <= 0L) {
                CommLog.error("PromotionLevelDeleteEvent clubId:{},Uid:{},Puid:{},dateTime:{}", getClubId(), getUid(), getPuid(), getDateTime());
                return;
            }
            CommLog.info("PromotionLevelDeleteEvent delete OldUid:{},OldPuid:{},newUid:{},newUpid:{}", getUid(), getPuid(), newUid, newUpid);
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
            map.put("memberId", newUid);
            map.put("upLevelId", newUpid);
            ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).update(map, Restrictions.and(Restrictions.eq("date_time", getDateTime()), Restrictions.eq("memberId", getUid())));
            ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).update(map, Restrictions.eq("memberId", getUid()));
            int hour = CommTime.getTodayHour();
            if (hour >= 0 && hour <= 1) {
                ContainerMgr.get().getComponent(ClubLevelRoomLogBeforeDayFlowService.class).update(map, Restrictions.and(Restrictions.eq("memberId", getUid())));
            }
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
