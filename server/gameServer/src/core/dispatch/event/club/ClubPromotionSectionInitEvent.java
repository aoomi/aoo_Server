package core.dispatch.event.club;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareclub.SharePromotionSectionMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.entity.clarkGame.PromotionShareSectionBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.db.service.clarkGame.PromotionShareSectionBOService;
import core.db.service.clarkGame.UnionShareSectionBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.PromotionShareSectionItem;
import jsproto.c2s.cclass.club.QueryUidOrPuidItem;
import jsproto.c2s.cclass.club.SharePromotionSection;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.cclass.union.UnionShareSectionItem;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 亲友圈初始化区间分成
 */
@Data
public class ClubPromotionSectionInitEvent implements BaseExecutor {
    /**
     * 亲友圈id
     */
    private long clubId;
    /**
     *
     */
    private long pid;
    /**
     * 亲友圈id
     */
    private long unionId;
    /**
     * type
     */
    private int shareType;
    private boolean execFlag = false;

    public ClubPromotionSectionInitEvent(long clubId, long pid, long unionId, int shareType) {
        this.setClubId(clubId);
        this.setPid(pid);
        this.setUnionId(unionId);
        this.setShareType(shareType);
    }

    @Override
    public void invoke() {
        Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
        if (Objects.isNull(club)) {
            CommLogD.error("club is null" + this.toString());
            return;
        }
        ClubMember clubMemberCreate = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, pid);
        if (Objects.isNull(clubMemberCreate)) {
            CommLogD.error("clubMemberCreate is null" + this.toString());
            club.setClubCreateShareFlag(false);
            return;
        }
        this.sectionInit(clubMemberCreate);
        ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId).values().stream().forEach(k -> {
//            if(k.isLevelPromotion()){
            this.sectionInit(k);
//            }
        });

        club.setClubCreateShareFlag(false);
    }

    /**
     * 初始化区间
     *
     * @param clubMember
     */

    private void sectionInit(ClubMember clubMember) {
        //设置分成方式为区间
        clubMember.getClubMemberBO().saveShareType(this.getShareType());
        if (this.getShareType() != UnionDefine.UNION_SHARE_TYPE.SECTION.ordinal()) {
            return;
        }
        List<UnionShareSectionItem> unionShareSectionItems = ((UnionShareSectionBOService) ContainerMgr.get().getComponent(UnionShareSectionBOService.class)).findAllE(Restrictions.eq("unionId", unionId),
                UnionShareSectionItem.class, UnionShareSectionItem.getItemsName());
        if (CollectionUtils.isEmpty(unionShareSectionItems)) {
            Union union;
            if (Config.isShare()) {
                union = ShareUnionListMgr.getInstance().getUnion(this.getUnionId());
            } else {
                union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionId());
            }
            if (Objects.isNull(union)) {
                return;
            }
            union.initUnionShareSection();
        }
        List<PromotionShareSectionItem> promotionShareSectionItems = ((PromotionShareSectionBOService) ContainerMgr.get().getComponent(PromotionShareSectionBOService.class)).findAllE(Restrictions.and(
                Restrictions.eq("pid", clubMember.getClubMemberBO().getPlayerID()), Restrictions.eq("clubId", clubMember.getClubMemberBO().getClubID())), PromotionShareSectionItem.class, PromotionShareSectionItem.getItemsName());
        //已经存在值的话就不用进行初始化
        if (CollectionUtils.isNotEmpty(promotionShareSectionItems)) {
            return;
        }
        int createTime = CommTime.nowSecond();
        //圈主往下进行分成
        List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
        List<QueryUidOrPuidItem> promotionList = queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).collect(Collectors.toList());
        for (UnionShareSectionItem unionShareSectionItem : unionShareSectionItems) {
            double allowShareToValue = unionShareSectionItem.getEndFlag() == 1 ? unionShareSectionItem.getBeginValue() : unionShareSectionItem.getEndValue();
            PromotionShareSectionBO promotionShareSectionBO = new PromotionShareSectionBO();
            promotionShareSectionBO.setClubId(this.getClubId());
            promotionShareSectionBO.setUnionSectionId(unionShareSectionItem.getId());
            promotionShareSectionBO.setPid(clubMember.getClubMemberBO().getPlayerID());
            promotionShareSectionBO.setCreateTime(createTime);
            promotionShareSectionBO.setBeginValue(unionShareSectionItem.getBeginValue());
            promotionShareSectionBO.setEndValue(unionShareSectionItem.getEndValue());
            promotionShareSectionBO.setEndFlag(unionShareSectionItem.getEndFlag());
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
                PromotionShareSectionItem item = promotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == unionShareSectionItem.getId()).findFirst().orElse(null);
                if (Objects.isNull(item)) {
                    continue;
                }
                allowShareToValue = CommMath.subDouble(allowShareToValue, item.getShareToSelfValue());
            }
            promotionShareSectionBO.setAllowShareToValue(allowShareToValue);
            promotionShareSectionBO.setShareToSelfValue(clubMember.isClubCreate() ? unionShareSectionItem.getEndValue() : allowShareToValue);
            ((PromotionShareSectionBOService) promotionShareSectionBO.getBaseService()).saveIgnoreOrUpDateInit(promotionShareSectionBO);
        }
        clubMember.initRedisSection();
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
        return "ClubPromotionSectionInitEvent{" +
                "clubId=" + clubId +
                ", pid=" + pid +
                ", unionId=" + unionId +
                '}';
    }
}
