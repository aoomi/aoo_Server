package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CClub_CompetitionRankedByStatusType extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     * 盟主操作 查看的那个亲友圈id(其他身份操作的是时候 opclubId和clubIdz值传一样的进来)
     */
    private long opClubId;
    /**
     * 俱乐部ID
     */
    private long unionId;
    /**
     * 查询的pid
     */
    private long pid;

    /**
     * 查询
     */
    private String query;
    /**
     *      * 0 全部 1未分配 2管理员 3普通
     */
    private int statusType;


}
