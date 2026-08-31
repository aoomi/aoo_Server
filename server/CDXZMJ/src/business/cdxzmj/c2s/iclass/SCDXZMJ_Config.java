package business.cdxzmj.c2s.iclass;				
				
import cenum.RoomTypeEnum;				
import jsproto.c2s.cclass.room.BaseCreateRoom;				
import jsproto.c2s.iclass.room.SBase_Config;				
				
@SuppressWarnings("serial")				
public class SCDXZMJ_Config extends SBase_Config {				
	public static SCDXZMJ_Config make(BaseCreateRoom cfg,RoomTypeEnum roomTypeEnum) {				
		SCDXZMJ_Config ret = new SCDXZMJ_Config();				
		ret.setCfg(cfg);				
		ret.setRoomType(roomTypeEnum);				
		return ret;				
	}				
}				
