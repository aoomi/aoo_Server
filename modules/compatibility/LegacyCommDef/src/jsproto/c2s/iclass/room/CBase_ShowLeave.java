package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CBase_ShowLeave extends BaseSendMsg {
    private long roomID;
    private boolean isShowLeave = false;


}
