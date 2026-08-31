package jsproto.c2s.iclass.club;

import lombok.Data;

@Data
public class CClub_FindPIDAdd {
    /**
     * 亲友圈Id
     */
    public long clubId;
    /**
     * 玩家pid
     */
    public int pid;
    public String query;
    private int type = 0;
    private int pageNum = 0;

    public static CClub_FindPIDAdd make(long clubId, int pid) {
        CClub_FindPIDAdd ret = new CClub_FindPIDAdd();
        ret.clubId = clubId;
        ret.pid = pid;
        return ret;
    }
}
