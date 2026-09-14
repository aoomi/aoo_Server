package jsproto.c2s.iclass.zjh;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.zjh.ZJHRoom_Set;

/**
 * 一局游戏开始
 * @author zaf
 * */
public class SZJH_SetStart extends BaseSendMsg {
    
    public long roomID;
    public ZJHRoom_Set setInfo;

    public static SZJH_SetStart make(long roomID, ZJHRoom_Set setInfo) {
        SZJH_SetStart ret = new SZJH_SetStart();
        ret.roomID = roomID;
        ret.setInfo = setInfo;      
        return ret;
    }
}