package business.xcpdk.c2s.iclass;

import cenum.RoomTypeEnum;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.iclass.room.SBase_Config;


@SuppressWarnings("serial")
public class SXCPDK_Config extends SBase_Config {
    public static SXCPDK_Config make(BaseCreateRoom cfg,RoomTypeEnum roomTypeEnum) {
    	SXCPDK_Config ret = new SXCPDK_Config();
        ret.setCfg(cfg);
        ret.setRoomType(roomTypeEnum);
        return ret;
    }
}
