package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * 重新设置玩家手牌
 *
 * @param <T>
 * @author Huaxing
 */
public class _SetPosCard<T> extends BaseSendMsg {
    public long roomID;
    // 每个玩家的牌面
    public List<T> setPosList = new ArrayList<>();

    public static <T> _SetPosCard make(long roomID, List<T> setPosList, String gameNameStr) {
        _SetPosCard ret = new _SetPosCard();
        ret.roomID = roomID;
        ret.setPosList = setPosList;
        ret.setGameNameStr(gameNameStr);
        return ret;


    }
}
