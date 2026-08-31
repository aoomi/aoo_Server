package jsproto.c2s.iclass.mj;

import cenum.RoomTypeEnum;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.iclass.room.SBase_Config;

@SuppressWarnings("serial")
public class _Config extends SBase_Config {
    public static _Config make(BaseCreateRoom cfg, RoomTypeEnum roomTypeEnum, String gameNameStr) {
        _Config ret = new _Config();
        ret.setCfg(cfg);
        ret.setRoomType(roomTypeEnum);
        ret.setGameNameStr(gameNameStr);
        return ret;
    }
}	
