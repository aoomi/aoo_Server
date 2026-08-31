package jsproto.c2s.iclass.union;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.union.UnionRoomCfgItem;

/**
 * 获取赛事游戏设置
 *
 * @author zaf
 */
public class SUnion_CreateGameSetChange extends BaseSendMsg {
    public long unionId;//俱乐部ID
    public UnionRoomCfgItem unionRoomCfgItem;//俱乐部房间状态
    public boolean isCreate;//是否是创建
    public long pid;//谁操作
    public int waitRoomCount;//等待中的房间
    public int playingRoomCount;//游戏中的房间

    public static SUnion_CreateGameSetChange make(long unionId, long pid, boolean isCreate, UnionRoomCfgItem unionRoomCfgItem
            , int waitRoomCount, int playingRoomCount) {
        SUnion_CreateGameSetChange ret = new SUnion_CreateGameSetChange();
        ret.unionId = unionId;
        ret.unionRoomCfgItem = unionRoomCfgItem;
        ret.isCreate = isCreate;
        ret.pid = pid;
        ret.waitRoomCount = waitRoomCount;
        ret.playingRoomCount = playingRoomCount;
        return ret;
    }
}