package jsproto.c2s.iclass.union;

import jsproto.c2s.cclass.union.UnionAlivePointRelease;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 生存任务发布
 */
@Data
public class CUnion_AlivePointRelease extends CUnion_Base {
    /***
     * 修改列表
     */
    private List<UnionAlivePointRelease> changeList=new ArrayList<>();

    public static CUnion_AlivePointRelease make(long clubId, List<UnionAlivePointRelease> changeList) {
        CUnion_AlivePointRelease ret = new CUnion_AlivePointRelease();
        ret.setClubId(clubId);
        ret.setChangeList(changeList);
        return ret;
    }
}
