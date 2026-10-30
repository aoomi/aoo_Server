package jsproto.c2s.iclass.zypk;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 一局游戏开始
 * @author zaf
 * @param <T>
 * */
public class SZYPK_SetStart<T> extends BaseSendMsg {

    public long roomID;
    public T setInfo;

    public static <T>SZYPK_SetStart make(long roomID, T setInfo) {
        SZYPK_SetStart ret = new SZYPK_SetStart();
        ret.roomID = roomID;
        ret.setInfo = setInfo;
        return ret;
    }
}
