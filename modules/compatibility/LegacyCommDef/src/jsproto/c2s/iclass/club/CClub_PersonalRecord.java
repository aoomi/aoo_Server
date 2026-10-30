package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.List;

@Data
public class CClub_PersonalRecord extends BaseSendMsg {
    private long clubId;//俱乐部编号
    private long unionId;
    private long pid;
    private List<Long> roomIDList;// 房间ID列表
    private boolean isAll;
    private int getType;
    private int pageNum;
    private long query;

    public static CClub_PersonalRecord make(long clubId, long pid, List<Long> roomIDList) {
        CClub_PersonalRecord ret = new CClub_PersonalRecord();
        ret.clubId = clubId;
        ret.pid = pid;
        ret.roomIDList = roomIDList;
        return ret;
    }
}