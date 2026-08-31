package core.dispatch.event.club;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareclub.SharePromotionSectionMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.entity.clarkGame.PromotionShareSectionBO;
import core.db.entity.clarkGame.UnionDynamicBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.ClubPromotionSectionCalcActiveItem;
import jsproto.c2s.cclass.club.PromotionShareSectionItem;
import jsproto.c2s.cclass.club.QueryUidOrPuidItem;
import jsproto.c2s.cclass.club.SharePromotionSection;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.club.CClub_PromotionSectionCalcActiveBatch;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 亲友圈初始化区间分成
 */
@Data
public class ClubPromotionSectionChangeEvent implements BaseExecutor {
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     * 修改的人的memberId
     */
    private long memberId;
    /**
     * 操作人
     */
    private long execPid;
    /**
     * 联盟id
     */
    private long unionId;
    /**
     * 修改的值
     */
    private CClub_PromotionSectionCalcActiveBatch promotionActive;

    public ClubPromotionSectionChangeEvent(long clubId, long memberId, long execPid, CClub_PromotionSectionCalcActiveBatch promotionActive, long unionId) {
        this.setClubId(clubId);
        this.setMemberId(memberId);
        this.setExecPid(execPid);
        this.setPromotionActive(promotionActive);
        this.setUnionId(unionId);
    }

    @Override
    public void invoke() {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        if (Objects.isNull(club)) {
            return;
        }
        ClubMember clubMember;
        if (Config.isShare()) {
            clubMember = ShareClubMemberMgr.getInstance().getClubMember(memberId);
        } else {
            clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(memberId);
        }
        if (Objects.isNull(clubMember)) {
            club.setSectionChangePromotionFlag(false);
            return;
        }
        //获取上级列表
        List<QueryUidOrPuidItem> queryUidOrPidItemListUpList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(
                Restrictions.eq("uid", memberId), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        if ((Objects.isNull(queryUidOrPidItemListUpList) || queryUidOrPidItemListUpList.isEmpty()) && !clubMember.isClubCreate()) {
            club.setSectionChangePromotionFlag(false);
            return;
        }
        //上级列表排序
        List<Long> promotionList = queryUidOrPidItemListUpList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).map(k -> k.getPuid()).collect(Collectors.toList());
        promotionList.add(memberId);
        //底下的人受到影响 都要进行相应的调整
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(
                Restrictions.eq("puid", memberId), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        List<Long> promotionListLower = queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId).reversed()).map(k -> k.getUid()).collect(Collectors.toList());
        if (Objects.nonNull(queryUidOrPidItemList) && !queryUidOrPidItemList.isEmpty()) {
            for (ClubPromotionSectionCalcActiveItem con : promotionActive.getPromotionSectionCalcActiveItems()) {
                //只针对有变的数据进行调整
//                if (con.isChangFlag()) {
                //盟主获得的为结束的值  计算过后得到本级可分配的值
                double totalShareValue = con.getEndValue();
                List<Long> ignoreList = new ArrayList<>();
                // 查询我的所有上线   从最上面那一层开始计算可分配值
                for (Long puid : promotionList) {
                    ClubMember promotionMember;
                    if (ignoreList.contains(puid)) {
                        continue;
                    }
                    if (Config.isShare()) {
                        promotionMember = ShareClubMemberMgr.getInstance().getClubMember(puid);
                    } else {
                        promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(puid);
                    }
                    if (Objects.nonNull(promotionMember)) {
                        SharePromotionSection sharePromotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(promotionMember.getId());
                        if (Objects.isNull(sharePromotionSection)) {
                            continue;
                        }
                        PromotionShareSectionItem promotionShareSectionItems = sharePromotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == con.getUnionSectionId()).findFirst().orElse(null);
                        if (Objects.isNull(promotionShareSectionItems)) {
                            continue;
                        }
                        totalShareValue = CommMath.subDouble(totalShareValue, promotionShareSectionItems.getShareToSelfValue());
                        ignoreList.add(promotionMember.getId());
                    }
                }
                //从下面开始算
                for (Long puidLower : promotionListLower) {
                    double upLevelTotalValue = totalShareValue;
                    if (ignoreList.contains(puidLower)) {
                        continue;
                    }
                    ClubMember promotionMember;
                    if (Config.isShare()) {
                        promotionMember = ShareClubMemberMgr.getInstance().getClubMember(puidLower);
                    } else {
                        promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(puidLower);
                    }
                    if (Objects.isNull(promotionMember)) {
                        continue;
                    }
                    List<QueryUidOrPuidItem> queryUidOrPidItemListUpListNormal = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(
                            Restrictions.eq("uid", promotionMember.getClubMemberBO().getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
                    //上级列表排序
                    List<Long> promotionListNormal = queryUidOrPidItemListUpListNormal.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).map(k -> k.getPuid()).collect(Collectors.toList());
                    promotionListNormal.add(puidLower);
                    boolean zeroFlag = false;
                    for (Long queryUidOrPuidItemNoraml : promotionListNormal) {
                        boolean saveFlag = false;
                        if (ignoreList.contains(queryUidOrPuidItemNoraml)) {
                            continue;
                        }
                        ClubMember promotionMemberNoraml;
                        if (Config.isShare()) {
                            promotionMemberNoraml = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItemNoraml);
                        } else {
                            promotionMemberNoraml = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItemNoraml);
                        }
                        if (Objects.isNull(promotionMemberNoraml)) {
                            continue;
                        }
                        SharePromotionSection sharePromotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(promotionMemberNoraml.getId());
                        if (Objects.isNull(sharePromotionSection)) {
                            continue;
                        }
                        PromotionShareSectionItem promotionShareSectionItems = sharePromotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == con.getUnionSectionId()).findFirst().orElse(null);
                        if (Objects.isNull(promotionShareSectionItems)) {
                            continue;
                        }
                        if (promotionShareSectionItems.getAllowShareToValue() != upLevelTotalValue) {
                            saveFlag = true;
                            promotionShareSectionItems.setAllowShareToValue(upLevelTotalValue);
                        }
                        double oldValue = promotionShareSectionItems.getShareToSelfValue();
                        if (zeroFlag) {
                            saveFlag = false;
                            promotionShareSectionItems.setShareToSelfValue(0);
                            this.savePromotionSectionInfo(promotionMemberNoraml, promotionShareSectionItems, oldValue, 0);
                        }
                        if (upLevelTotalValue >= promotionShareSectionItems.getShareToSelfValue()) {
                            upLevelTotalValue = CommMath.subDouble(upLevelTotalValue, promotionShareSectionItems.getShareToSelfValue());
                        } else {
                            zeroFlag = true;
                            saveFlag = false;
                            promotionShareSectionItems.setShareToSelfValue(upLevelTotalValue);
                            this.savePromotionSectionInfo(promotionMemberNoraml, promotionShareSectionItems, oldValue, upLevelTotalValue);
                        }
                        if (saveFlag) {
                            this.savePromotionSectionInfo(promotionMemberNoraml, promotionShareSectionItems, oldValue, upLevelTotalValue);
                        }
                        ignoreList.add(queryUidOrPuidItemNoraml);
                    }
                }
//                }
            }

        }
        club.setSectionChangePromotionFlag(false);
    }


    private void savePromotionSectionInfo(ClubMember promotionMemberNoraml, PromotionShareSectionItem promotionShareSectionItems, double oldValue, double newValue) {
        double oldAllowShareValue = promotionShareSectionItems.getAllowShareToValue();
        double allowShareToValue = promotionShareSectionItems.getEndFlag() == 1 ? promotionShareSectionItems.getBeginValue() : promotionShareSectionItems.getEndValue();
        //圈主往下进行分成
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", promotionMemberNoraml.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        List<QueryUidOrPuidItem> promotionList = queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).collect(Collectors.toList());
        for (QueryUidOrPuidItem queryUidOrPuidItem : promotionList) {
            if (allowShareToValue <= 0) {
                break;
            }
            ClubMember promotionMember;
            if (Config.isShare()) {
                promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getPuid());
            } else {
                promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getPuid());
            }
            if (Objects.isNull(promotionMember) || (!promotionMember.isLevelPromotion() && !promotionMember.isClubCreate())) {
                continue;
            }
            SharePromotionSection promotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(promotionMember.getId());
            if (Objects.isNull(promotionSection)) {
                continue;
            }
            PromotionShareSectionItem item = promotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == promotionShareSectionItems.getUnionSectionId()).findFirst().orElse(null);
            if (Objects.isNull(item)) {
                continue;
            }
            allowShareToValue = CommMath.subDouble(allowShareToValue, item.getShareToSelfValue());
        }
        PromotionShareSectionBO promotionShareSectionBO = new PromotionShareSectionBO();
        promotionShareSectionBO.setId(promotionShareSectionItems.getId());
        promotionShareSectionBO.setClubId(promotionShareSectionItems.getClubId());
        promotionShareSectionBO.setUnionSectionId(promotionShareSectionItems.getUnionSectionId());
        promotionShareSectionBO.setPid(promotionShareSectionItems.getPid());
        promotionShareSectionBO.setShareToSelfValue(promotionShareSectionItems.getShareToSelfValue());
        promotionShareSectionBO.setBeginValue(promotionShareSectionItems.getBeginValue());
        promotionShareSectionBO.setEndValue(promotionShareSectionItems.getEndValue());
        promotionShareSectionBO.setEndFlag(promotionShareSectionItems.getEndFlag());
        promotionShareSectionBO.setAllowShareToValue(allowShareToValue);
        promotionShareSectionBO.getBaseService().saveIgnoreOrUpDate(promotionShareSectionBO);
        promotionMemberNoraml.initRedisSection();
        UnionDynamicBO.insertSportsPoint(promotionMemberNoraml.getClubMemberBO().getPlayerID(), unionId, promotionActive.getOpClubId(), 0, 0, CommTime.nowSecond(),
                UnionDefine.UNION_EXEC_TYPE.PROMOTION_EXEC_SHARE_SECTION_CHANGE.value(), "", 2, String.valueOf(promotionShareSectionBO.getEndValue()), String.valueOf(promotionShareSectionBO.getBeginValue()),
                String.valueOf(newValue),
                String.valueOf(oldValue), String.valueOf(execPid), "", "", "");

        if (oldAllowShareValue != allowShareToValue) {
            UnionDynamicBO.insertSportsPoint(promotionMemberNoraml.getClubMemberBO().getPlayerID(), unionId, promotionActive.getOpClubId(), promotionActive.getOpPid(), promotionActive.getOpClubId(), CommTime.nowSecond(),
                    UnionDefine.UNION_EXEC_TYPE.PROMOTION_EXEC_SHARE_SECTION_ALLOW_CHANGE.value(), "", 1, String.valueOf(promotionShareSectionBO.getEndValue()), String.valueOf(promotionShareSectionBO.getBeginValue()),
                    String.valueOf(allowShareToValue),
                    String.valueOf(oldAllowShareValue), String.valueOf(execPid), "", "", "");
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.PROMOTION_SECTION_INIT.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PROMOTION_SECTION_INIT.bufferSize();
    }

    @Override
    public String toString() {
        return "ClubPromotionSectionChangeEvent{" +
                "clubId=" + clubId +
                ", memberId=" + memberId +
                ", execPid=" + execPid +
                ", promotionActive=" + promotionActive +
                '}';
    }
}
