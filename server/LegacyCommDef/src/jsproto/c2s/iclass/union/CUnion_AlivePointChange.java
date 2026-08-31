package jsproto.c2s.iclass.union;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CUnion_AlivePointChange extends CUnion_Base {


    /**
     * 生存积分状态（0:不开启,1:开启）
     */
    private int alivePointStatus;
    /**
     * 设置的值
     */
    private double value;

    /**
     * 操作亲友圈Id
     */
    private long opClubId;
    /**
     * 操作玩家
     */
    private long opPid;
    /**
     * 操作玩家
     */
    private long pid;
    /**
     * 要修改的玩家
     * 批量修改
     */
    private List<Long> pidList = new ArrayList<>();

    public static CUnion_AlivePointChange make(long clubId, long pid) {
        CUnion_AlivePointChange ret = new CUnion_AlivePointChange();
        ret.setClubId(clubId);
        ret.setOpPid(pid);
        return ret;
    }
}
