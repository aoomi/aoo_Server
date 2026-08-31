package core.dispatch.event.promotion;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubListMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.player.PlayerMgr;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import core.db.entity.clarkGame.ClubMemberRelationBO;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.db.service.clarkLog.ClubLevelRoomCountLogFlowService;
import core.db.service.clarkLog.ClubLevelRoomLogBeforeDayFlowService;
import core.db.service.clarkLog.ClubLevelRoomLogFlowService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import jsproto.c2s.cclass.club.QueryUidOrPuidItem;
import jsproto.c2s.cclass.union.UnionDefine;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代理等级
 */
@Data
public class PromotionLevelChangeEvent implements BaseExecutor {
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     * 亲友圈成员Id
     * 变更归属的那个推广员
     */
    private long uid;
    /**
     * 上级亲友圈成员Id
     * 变更到所属的那个推广员的id
     */
    private long puid;
    /**
     * 时间
     */
    private String dateTime;
    /**
     * 变更归属的那个推广员的所有下线
     */
    private List<Long> uidList;
    private long doPid;
    /**
     * 联盟id
     */
    private long unionId;
    /**
     * 类型修改变化
     */
    private boolean shareTypeChangeFlag;

    private long oldUpLevelId;

    public PromotionLevelChangeEvent(long clubId, long uid, long puid, String dateTime, List<Long> uidList, long doPid, long unionId) {
        this.setClubId(clubId);
        this.setUid(uid);
        this.setPuid(puid);
        this.setDateTime(dateTime);
        this.setUidList(uidList);
        this.setDoPid(doPid);
        this.setUnionId(unionId);
    }

    @Override
    public void invoke() {

        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        ClubMember changeClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, uid);
        //如果找不到的话 直接退出
        if (Objects.isNull(changeClubMember)) {
            return;
        }
        this.setOldUpLevelId(changeClubMember.getClubMemberBO().getUpLevelId());
        ClubMember upClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, puid);
        //如果找不到的话 直接退出
        if (Objects.isNull(changeClubMember)) {
            return;
        }
        // 我的上级
        long oldUpLevel = changeClubMember.getClubMemberBO().getUpLevelId();
        ClubMember oldUpClubMember;
        if (Config.isShare()) {
            oldUpClubMember = ShareClubMemberMgr.getInstance().getClubMember(oldUpLevel);
        } else {
            oldUpClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(oldUpLevel);
        }
        //找不到上级的话就把记录扔到亲友圈圈主身上
        long oldUpClubMemberUpLevel = 0L;
        if (Objects.isNull(oldUpClubMember)) {
            // 创建者
            oldUpClubMemberUpLevel = ClubMgr.getInstance().getClubMemberMgr().getClubCreateMemberId(clubId);
        } else {
            // 我上级的上级
            oldUpClubMemberUpLevel = oldUpClubMember.getClubMemberBO().getUpLevelId();
        }
        final long oldUpClubMemberUpLevelFinal = oldUpClubMemberUpLevel;
        int exeOldNum = uidList.size() - 1;//要去除掉自己
        int oldNum = getPromotionNextNum(upClubMember.getId());
        List<Long> upPidList = getUidList(upClubMember.getId());
        List<ClubMember> clubMembers;
        if (Config.isShare()) {
            clubMembers = uidList.stream().filter(k -> Objects.nonNull(ShareClubMemberMgr.getInstance().getClubMember(k.longValue()))).map(k -> {
                return ShareClubMemberMgr.getInstance().getClubMember(k.longValue());
            }).collect(Collectors.toList());
        } else {
            clubMembers = uidList.stream().filter(k -> Objects.nonNull(ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(k.longValue()))).map(k -> {
                return ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(k.longValue());
            }).collect(Collectors.toList());
        }
        //如果找到的人数 和之前的人数不一样的话 说明中间有人被删除了 当前的修改就不进行
        if (uidList.size() != clubMembers.size()) {
            club.setMultiChangePromotionFlag(false);
            //共享亲友圈更新
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().updateField(club, "multiChangePromotionFlag");
            }
            return;
        }
        //获得等级变化的值
        int changeLevel = upClubMember.isClubCreate() ? changeClubMember.getLevel() - 1 : changeClubMember.getLevel() - upClubMember.getLevel() - 1;
        //删除  所有下级的uid绑定的数据
        ContainerMgr.get().getComponent(ClubMemberRelationBOService.class).delete(Restrictions.in("uid", uidList));
        CommLogD.error("PromotionLevelChangeEvent delete  uid in:" + uidList);
        // 更新归属
        ClubMgr.getInstance().getClubMemberMgr().updateClubMemberUpLevelIdByPid(changeClubMember.getClubMemberBO().getId(), upClubMember.getClubMemberBO().getId());
        clubMembers.stream().forEach(k -> {
            if (Config.isShare()) {
                k = ShareClubMemberMgr.getInstance().getClubMember(k.getClubMemberBO().getId());
            }
            //如果这个人是推广员的话 那么推广员等级也要跟着变
            if (k.isLevelPromotion()) {
                k.getClubMemberBO().saveLevel(k.getClubMemberBO().getLevel() - changeLevel);
                this.changeShareType(k, upClubMember);
            }
            insertClubMemberRelation(k.getId(), k.getClubMemberBO().getUpLevelId());
            Map<String, Object> map = Maps.newHashMapWithExpectedSize(2);
            map.put("memberId", oldUpLevel);
            map.put("upLevelId", oldUpClubMemberUpLevelFinal);
            ContainerMgr.get().getComponent(ClubLevelRoomLogFlowService.class).update(map, Restrictions.and(Restrictions.eq("date_time", getDateTime()), Restrictions.eq("memberId", k.getId())));
            ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).update(map, Restrictions.and(Restrictions.eq("memberId", k.getId()), Restrictions.ge("date_time", CommTime.getYesterDayStringYMD(8))));
            int hour = CommTime.getTodayHour();
            if (hour >= 0 && hour <= 1) {
                ContainerMgr.get().getComponent(ClubLevelRoomLogBeforeDayFlowService.class).update(map, Restrictions.and(Restrictions.eq("memberId", k.getId())));
            }
        });
        if (upClubMember.isClubCreate() && !changeClubMember.isLevelPromotion()) {
            ClubMgr.getInstance().getClubMemberMgr().updateClubMemberUpLevelIdByPid(changeClubMember.getClubMemberBO().getId(), 0);
        }
        recordLog(exeOldNum, oldNum, upPidList);
        //变更成功后 重置推广员管理状态
        changeClubMember.getClubMemberBO().savePromotionManage(0);
        club.setMultiChangePromotionFlag(false);
        //共享亲友圈更新
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().updateField(club, "multiChangePromotionFlag");
        }

    }

    /**
     * 分成类型变化
     *
     * @param k
     * @param upClubMember
     */
    private void changeShareType(ClubMember k, ClubMember upClubMember) {
        if (k.getClubMemberBO().getShareType() != upClubMember.getClubMemberBO().getShareType() || this.isShareTypeChangeFlag()) {
            this.setShareTypeChangeFlag(true);
            Double oldValue = new Double(k.getClubMemberBO().getShareFixedValue());
            double oldFixedValue = new Double(k.getClubMemberBO().getShareFixedValue());
            k.getClubMemberBO().saveShareValue(0, upClubMember.getClubMemberBO().getShareType());
            UnionDynamicBO.insertRoomSportsPoint(k.getClubMemberBO().getPlayerID(), k.getClubID(), upClubMember.getClubMemberBO().getPlayerID(), k.getClubID(), CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_SCORE_PERCENT.value(), this.unionId, upClubMember.getClubMemberBO().getShareType() == 0 ? String.valueOf((int) 0 + "%") : String.valueOf(0), upClubMember.getClubMemberBO().getShareType() == 0 ? String.valueOf(oldValue.intValue() + "%") : String.valueOf(oldFixedValue), "");
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

    /**
     * 获取名下有多少人
     *
     * @param pid
     * @return
     */
    public int getPromotionNextNum(long pid) {
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", pid), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        int num = 0;
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            num = queryUidOrPidItemList.size();
        }
        return num;
    }

    /**
     * 防止队列阻塞 执行顺序混乱 单独拿出来
     *
     * @param uid
     * @param puid
     */
    public void insertClubMemberRelation(Long uid, Long puid) {
        String sql = String.format("INSERT  IGNORE  INTO `ClubMemberRelation`(`uid`, `puid`) SELECT ? AS uid,puid FROM ClubMemberRelation WHERE uid = ?", uid, puid);
        ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).insert(sql, Arrays.asList(uid, puid).toArray(new Object[2]));
        ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).saveIgnore(new ClubMemberRelationBO(uid, puid));
    }

    /**
     * 记录log
     * 防止队列阻塞 执行顺序混乱 单独拿出来
     *
     * @param exeOldNum
     * @param oldNum
     */
    public void recordLog(int exeOldNum, int oldNum, List<Long> upPidList) {
        ClubMember changeClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, uid);
        //如果找不到的话 直接退出
        if (Objects.isNull(changeClubMember)) {
            return;
        }
        ClubMember upClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, puid);
        //如果找不到的话 直接退出
        if (Objects.isNull(changeClubMember)) {
            return;
        }
        int exeNowNum = getPromotionNextNum(changeClubMember.getId());
        int nowNum = getPromotionNextNum(upClubMember.getId());
        List<Long> pidListNow = getUidList(changeClubMember.getId());
        List<Long> upPidListNow = getUidList(upClubMember.getId());
        business.player.Player player = PlayerMgr.getInstance().getPlayer(puid);
        UnionDynamicBO.insertPromotionBelongChange(uid, clubId, doPid, CommTime.nowSecond(), UnionDefine.UNION_EXEC_TYPE.PROMOTION_BELONG_CHANGE.value(),
                unionId, String.valueOf(puid), player.getName());

        FlowLogger.promotionMultiChangLog(upClubMember.getClubMemberBO().getPlayerID(), clubId, changeClubMember.getClubMemberBO().getPlayerID(), oldNum, nowNum, exeOldNum, exeNowNum, doPid, uidList.toString(), pidListNow.toString(), upPidList.toString(), upPidListNow.toString());
        //直属上级添加一条竞技动态
        ClubMember upMemberOld = ShareClubMemberMgr.getInstance().getClubMember(this.getOldUpLevelId());
        ClubMember upMemberNow = ShareClubMemberMgr.getInstance().getClubMember(changeClubMember.getClubMemberBO().getUpLevelId());
        if (Objects.nonNull(upMemberOld) && Objects.nonNull(upMemberNow)) {
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(upMemberOld.getClubMemberBO().getClubID());
            SharePlayer doPlayer = SharePlayerMgr.getInstance().getSharePlayer(doPid);
            SharePlayer upPlayerNow = SharePlayerMgr.getInstance().getSharePlayer(upMemberNow.getClubMemberBO().getPlayerID());
            if (Objects.nonNull(club) && Objects.nonNull(doPlayer) && Objects.nonNull(upPlayerNow)) {
                UnionDynamicBO.insertClubDynamic(uid, clubId, upMemberOld.getClubMemberBO().getPlayerID(), CommTime.nowSecond(),
                        UnionDefine.UNION_EXEC_TYPE.CLUB_ZHI_SHU_CHANGE_BELONG.value(), club.getClubListBO().getUnionId(),
                        String.valueOf(doPlayer.getPlayerBO().getId()), doPlayer.getPlayerBO().getName(), String.valueOf(upPlayerNow.getPlayerBO().getId()), upPlayerNow.getPlayerBO().getName());
            }
        }
    }

    /**
     * 获取传进来的人所有下线
     *
     * @param puid
     * @return
     */
    public List<Long> getUidList(Long puid) {
        List<Long> uidList = Lists.newArrayList();//所有下线的list
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", puid), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        if (CollectionUtils.isNotEmpty(queryUidOrPidItemList)) {
            // 添加所有下线：
            uidList.addAll(queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).map(k -> k.getUid()).collect(Collectors.toList()));
        }
        return uidList;
    }
}
