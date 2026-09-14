package core.dispatch.event.union;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.union.Union;
import business.utils.TimeConditionUtils;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.google.common.collect.Lists;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.db.service.clarkLog.ClubLevelRoomCountLogFlowService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.ClubPromotionLevelReportFormItem;
import jsproto.c2s.cclass.club.QueryUidOrPuidItem;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.club.CClub_SportsPointExamine;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Data
public class UnionExamineAutoEvent implements BaseExecutor {


    public UnionExamineAutoEvent() {
    }

    @Override
    public void invoke() {
        //找出所有的联盟
        Map<Long, Union> allUnion = ShareUnionListMgr.getInstance().getAllUnion();
        for (Map.Entry<Long, Union> union : allUnion.entrySet()) {
            Union unionCon = union.getValue();
            //符合条件的联盟
            if (UnionDefine.UNION_WARN_EXAMINE.AUTO.ordinal() == unionCon.getUnionBO().getExamineStatus()) {
                List<Long> clubIdList = ClubMgr.getInstance().getClubListMgr().getClubIdListByUnion(unionCon.getUnionBO().getId());
                for (Long clubId : clubIdList) {
                    Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubId);
                    ClubMember ownerClubMember = ShareClubMemberMgr.getInstance().getClubMember(club.getClubListBO().getId(), club.getOwnerPlayerId());
                    if (Objects.isNull(ownerClubMember)) {
                        continue;
                    }
                    Map<Long, ClubMember> clubMemberMap = ShareClubMemberMgr.getInstance().getAllOneClubMember(clubId);

                    for (Map.Entry<Long, ClubMember> clubMemberEntry : clubMemberMap.entrySet()) {
                        ClubMember clubMember = clubMemberEntry.getValue();
                        if (clubMember.isLevelPromotion()) {
                            ClubMember upClubMember = ShareClubMemberMgr.getInstance().getClubMember(clubMember.getClubMemberBO().getUpLevelId());
                            if (Objects.isNull(upClubMember)) {
                                upClubMember = ownerClubMember;
                            }
                            double totalPoint = 0;
                            List<Long> uidList = Lists.newArrayList();
                            uidList.add(clubMember.getId());
                            List<QueryUidOrPuidItem> queryUidOrPuidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("puid", clubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameUid());
                            if (CollectionUtils.isNotEmpty(queryUidOrPuidItemList)) {
                                // 查询我的所有下线（包括我）：
                                uidList.addAll(queryUidOrPuidItemList.stream().map(v -> v.getUid()).collect(Collectors.toList()));
                            }
                            if (CollectionUtils.isNotEmpty(uidList)) {
                                Criteria zeroClockS = TimeConditionUtils.CLUBDayZeroClockS("date_time", 1);
                                ClubPromotionLevelReportFormItem clubPromotionLevelItem = ContainerMgr.get().getComponent(ClubLevelRoomCountLogFlowService.class).
                                        findOneE(Restrictions.and(zeroClockS, Restrictions.eq("clubID", clubId),
                                                Restrictions.in("memberId", uidList)), ClubPromotionLevelReportFormItem.class, ClubPromotionLevelReportFormItem.getItemsName());
                                if (Objects.nonNull(clubPromotionLevelItem)) {
                                    totalPoint = clubPromotionLevelItem.getSportsPointConsume() + clubPromotionLevelItem.getPromotionShareValue() - clubPromotionLevelItem.getActualEntryFee();
                                }
                            }
                            CommLogD.error(new CClub_SportsPointExamine(clubId, clubMember.getClubMemberBO().getPlayerID(), 0, totalPoint, 1, clubId).toString());
                            ClubMgr.getInstance().getClubMemberMgr().execSportsPointExamine(new CClub_SportsPointExamine(clubId, clubMember.getClubMemberBO().getPlayerID(), 0, totalPoint, 1, clubId), upClubMember.getClubMemberBO().getPlayerID(), true);
                        }
                    }
                }
            }
        }


    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.UNION_EXAMINE.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.UNION_EXAMINE.bufferSize();
    }
}
