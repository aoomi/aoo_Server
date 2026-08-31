package business.njpdk.c2s.iclass;

import cenum.RoomTypeEnum;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.iclass.room.SBase_Config;


public class SNJPDK_Config<T> extends SBase_Config {

    public static SNJPDK_Config make(BaseCreateRoom cfg, RoomTypeEnum roomTypeEnum) {
        SNJPDK_Config ret = new SNJPDK_Config();
        ret.setCfg(cfg);
        ret.setRoomType(roomTypeEnum);
        return ret;


    }
}
