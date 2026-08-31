package core.dispatch.event.room;

import BaseCommon.CommLog;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.entity.clarkGame.ClubMemberBO;
import core.db.entity.clarkGame.PromotionDynamicBO;
import core.db.service.clarkGame.RoomConfigCalcActiveBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.Club_define;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 已废弃
 */
@Deprecated
@Data
public class RoomPromotionActiveEvent implements BaseExecutor {
    /**
     * 推广员pid
     */
    private long partnerPid;
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     * 房间key
     */
    private String roomKey;
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 配置Id
     */
    private long configId;
    /**
     * 亲友圈成员列表
     */
    private List<ClubMemberBO> clubMemberList;

    public RoomPromotionActiveEvent(long partnerPid, long clubId, long unionId, long configId, String roomKey, List<ClubMemberBO> clubMemberList) {
        this.partnerPid = partnerPid;
        this.clubId = clubId;
        this.roomKey = roomKey;
        this.unionId = unionId;
        this.configId = configId;
        this.clubMemberList = clubMemberList;
    }

    @Override
    public void invoke() {
        ClubMember promotion = ClubMgr.getInstance().getClubMemberMgr().getPromotion(getClubId(), getPartnerPid());
        if (Objects.isNull(promotion)) {
            CommLogD.error("PromotionActiveReportFormEvent roomKey:{},clubId:{},partnerPid:{}", getRoomKey(), getClubId(), getPartnerPid());
            return;
        }
        if (CollectionUtils.isEmpty(getClubMemberList())) {
            CommLogD.error("PromotionActiveReportFormEvent roomKey:{},clubId:{},partnerPid:{} isEmpty ClubMemberList", getRoomKey(), getClubId(), getPartnerPid());
            return;
        }
        if (promotion.isAppointPromotion()) {
            // 任命的推广员
            double value = ContainerMgr.get().getComponent(RoomConfigCalcActiveBOService.class).findScorePercen(getPartnerPid(), getUnionId(), getClubId(), getConfigId(), promotion.getClubMemberBO().getCalcActive());
            if (value <= 0D) {
                return;
            }
            // 总活跃值
            double sumValue = 0D;
            for (ClubMemberBO k : getClubMemberList()) {
                if (k.getPartnerPid() == getPartnerPid()) {
                    // 当前活跃值
                    double curValue = k.execPromotionActiveValue(value);
                    // 计算总活跃值
                    sumValue = CommMath.addDouble(sumValue, value);
                    // 	房间活跃度获取：年-月-日 时-分-秒  通过下属@玩家名称参与房间@房间号 对局获得@值 活跃度，当前活跃度@值；
                    PromotionDynamicBO.insertPromotionDynamicBO(getClubId(), getUnionId(), k.getPlayerID(), 0L, Club_define.Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_ACTIVE_GET.value(), String.valueOf(value), String.valueOf(curValue), getRoomKey(), getPartnerPid());
                    CommLog.info("insertPromotionDynamicBO");
                } else {
                    CommLog.info("insertPromotionDynamicBO k.getPartnerPid:{},PartnerPid:{}", k.getPartnerPid(), getPartnerPid());

                }
            }
            promotion.getClubMemberBO().execSumPromotionActiveValue(sumValue);
        } else {
            CommLogD.error("PromotionActiveReportFormEvent roomKey:{},clubId:{},partnerPid:{} isNotAppointPromotion", getRoomKey(), getClubId(), getPartnerPid());
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.ROOM.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.ROOM.bufferSize();
    }
}
