package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.List;

@Data
public class CClub_PromotionShowList extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;


    /**
     *
     */
    private List<Integer> showList;
    /**
     * 二级菜单显示
     */
    private List<Integer> showListSecond;

    public static CClub_PromotionShowList make(long clubId, List<Integer> showList, List<Integer> showListSecond) {
        CClub_PromotionShowList ret = new CClub_PromotionShowList();
        ret.setClubId(clubId);
        ret.setShowList(showList);
        ret.setShowListSecond(showListSecond);
        return ret;
    }
}
