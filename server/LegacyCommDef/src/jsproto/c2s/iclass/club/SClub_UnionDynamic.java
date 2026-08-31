package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.union.UnionDynamicItem;

import java.util.List;

/**
 * 获取竞技动态类型
 *
 * @author zaf
 */
public class SClub_UnionDynamic extends BaseSendMsg {

    public List<UnionDynamicItem> unionDynamicItemList;
    public int pageNum;

    public static SClub_UnionDynamic make(List<UnionDynamicItem> unionDynamicItemList, int pageNum) {
        SClub_UnionDynamic ret = new SClub_UnionDynamic();
        ret.unionDynamicItemList = unionDynamicItemList;
        ret.pageNum = pageNum;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}