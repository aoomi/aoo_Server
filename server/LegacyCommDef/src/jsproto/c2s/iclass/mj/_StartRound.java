package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 没轮开始消息
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public class _StartRound<T> extends BaseSendMsg {

    public long roomID;
    public T room_SetWait;


    public static <T> _StartRound<T> make(long roomID, T room_SetWait, String gameNameStr) {
        _StartRound ret = new _StartRound();
        ret.roomID = roomID;
        ret.room_SetWait = room_SetWait;
        ret.setGameNameStr(gameNameStr);
        return ret;
    }
}
