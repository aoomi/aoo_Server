package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CClub_MemberOpZhongZhi extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;


    /**
     * 查询的pid
     */
    private long pid;

    public CClub_MemberOpZhongZhi(long clubId, long pid) {
        this.clubId = clubId;
        this.pid = pid;
    }
}
