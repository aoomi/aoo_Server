package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 继续房间
 *
 * @author xushaojun
 */
@Data
public class CBase_ContinueRoom extends BaseSendMsg {
    public long roomID;
    public int continueType;

    public static CBase_ContinueRoom make(long roomID, int continueType) {
        CBase_ContinueRoom ret = new CBase_ContinueRoom();
        ret.roomID = roomID;
        ret.continueType = continueType;
        return ret;
    }


}