package jsproto.c2s.iclass.union;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CUnion_EliminatePointChange extends CUnion_Base {


    /**
     * 设置的值
     */
    private double value;

    /**
     * 操作俱乐部Id
     */
    private long opClubId;
    /**
     * 操作成员
     */
    private long opPid;
    /**
     * 操作成员
     */
    private long pid;
    /**
     * 要修改的成员
     * 批量修改
     */
    private List<Long> pidList = new ArrayList<>();

    public static CUnion_EliminatePointChange make(long clubId, long pid) {
        CUnion_EliminatePointChange ret = new CUnion_EliminatePointChange();
        ret.setClubId(clubId);
        ret.setOpPid(pid);
        return ret;
    }
}
