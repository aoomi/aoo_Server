package business.scjymj.c2s.iclass;

import cenum.room.SetState;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 接收客户端数据
 * 状态改变
 *
 * @author zaf
 */

@SuppressWarnings("serial")
public class SSCJYMJ_ChangeStatus extends BaseSendMsg {

    public long roomID;
    public int setID;//局数
    public SetState state;  //位置

    public static SSCJYMJ_ChangeStatus make(long roomID, int setID, SetState state) {
        SSCJYMJ_ChangeStatus ret = new SSCJYMJ_ChangeStatus();
        ret.roomID = roomID;
        ret.setID = setID;
        ret.state = state;
        return ret;
    }
}
