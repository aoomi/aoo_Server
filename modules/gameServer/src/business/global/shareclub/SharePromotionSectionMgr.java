package business.global.shareclub;

import business.global.club.Club;
import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import com.ddm.server.common.redis.RedisMap;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.club.SharePromotionSection;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Objects;

/**
 * @author FengZhnag
 * @date 2021/5/17 16:06
 * @description 共享区间管理
 */
public class SharePromotionSectionMgr {
    private static final String SHARE_PROMOTION_SECTION = "sharePromotionSectionKey";
    //test
    private static SharePromotionSectionMgr instance = new SharePromotionSectionMgr();

    // 获取单例
    public static SharePromotionSectionMgr getInstance() {
        return instance;
    }

    /**
     * 添加共享俱乐部区间信息
     *
     * @param clubMember
     */
    public void addClubMemberPromotionSection(ClubMember clubMember, SharePromotionSection sharePromotionSection) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_PROMOTION_SECTION);
        redisMap.putJson(String.valueOf(clubMember.getClubMemberBO().getId()), sharePromotionSection);
    }

    /**
     * 获取成员区间信息
     *
     * @param id
     * @return
     */
    public SharePromotionSection getClubMemberPromotionSection(Long id) {
        RedisMap redisMap = ContainerMgr.get().getRedis().getMap(SHARE_PROMOTION_SECTION);
        SharePromotionSection sharePromotionSection = redisMap.getObject(String.valueOf(id), SharePromotionSection.class);
        //如果找不到数据 重新初始化生
        if (Objects.isNull(sharePromotionSection) || CollectionUtils.isEmpty(sharePromotionSection.getPromotionShareSectionItems())) {
            ClubMember clubMember = ShareClubMemberMgr.getInstance().getClubMember(id);
            if (Objects.isNull(clubMember)) {
                return null;
            }
            Club club = ClubMgr.getInstance().getClubListMgr().findClub(clubMember.getClubID());
            if (Objects.isNull(club)) {
                return null;
            }
            clubMember.initPromotionSection(club.getClubListBO().getUnionId(), clubMember.isClubCreate());
            return getClubMemberPromotionSection(id);
        }
        return sharePromotionSection;
    }
}
