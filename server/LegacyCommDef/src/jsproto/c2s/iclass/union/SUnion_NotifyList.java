package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.List;

@Data
public class SUnion_NotifyList extends BaseSendMsg {
    private List<SUnion_Invited> unionInvitedList;

    public static SUnion_NotifyList make(List<SUnion_Invited> unionInvitedList) {
        SUnion_NotifyList ret = new SUnion_NotifyList();
        ret.setUnionInvitedList(unionInvitedList);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
