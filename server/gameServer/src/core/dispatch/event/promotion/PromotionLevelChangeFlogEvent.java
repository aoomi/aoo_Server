package core.dispatch.event.promotion;

import cenum.DispatcherComponentEnum;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.QueryUidOrPuidItem;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理等级批量变换 数据库记录
 * 暂时不做在队列中
 */
@Data
public class PromotionLevelChangeFlogEvent implements BaseExecutor {
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     * 亲友圈成员Id
     * 变更归属的那个推广员
     */
    private long pid;
    /**
     * 上级亲友圈成员Id
     * 变更到所属的那个推广员的id
     */
    private long exePid;

    private int exeOldNum;
    private int oldNum;
    private long doPid;
    private List<Long> changePidList = new ArrayList<>();

    public PromotionLevelChangeFlogEvent(long clubId, long pid, long exePid, int exeOldNum, int oldNum, long doPid, List<Long> changePidList) {
        this.clubId = clubId;
        this.pid = pid;
        this.exePid = exePid;
        this.exeOldNum = exeOldNum;
        this.oldNum = oldNum;
        this.doPid = doPid;
        this.changePidList = changePidList;
    }

    @Override
    public void invoke() {
//        ClubMember changeClubMember=ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId,pid);
//        //如果找不到的话 直接退出
//        if(Objects.isNull(changeClubMember)){
//            return;
//        }
//        ClubMember upClubMember=ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId,exePid);
//        //如果找不到的话 直接退出
//        if(Objects.isNull(changeClubMember)){
//            return;
//        }
//        int exeNowNum=getPromotionNextNum(changeClubMember.getId());
//        int nowNum=getPromotionNextNum(upClubMember.getId());
//        FlowLogger.promotionMultiChangLog(upClubMember.getClubMemberBO().getPlayerID(),clubId,changeClubMember.getClubMemberBO().getPlayerID(),oldNum,nowNum,exeOldNum,exeNowNum,doPid,changePidList.toString());

    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.PROMOTION_LEVEL.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PROMOTION_LEVEL.bufferSize();
    }

    public int getPromotionNextNum(long pid) {
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", pid), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        int num = 0;
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            num = queryUidOrPidItemList.size();
        }
        return num;
    }

}
