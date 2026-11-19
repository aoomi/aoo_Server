package jsproto.c2s.iclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;


@Data
public class CUnion_GetMemberManage extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public long unionId;//联赛ID
    public long queryPid;//查询的id
    public long queryClubId;//查询的俱乐部ID


}