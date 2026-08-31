package jsproto.c2s.iclass.zypk;
import java.util.List;

import cenum.room.RoomState;
import cenum.PrizeType;
import jsproto.c2s.cclass.room.GetRoomInfo;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.cclass.room.Room_Dissolve;
import jsproto.c2s.cclass.zypk.ZYPKRoom_Cfg;
import jsproto.c2s.cclass.zypk.ZYPKRoom_Set;

/*
 * 用户信息
 * @author huaxing
 * */
public class SZYPK_GetRoomInfo extends GetRoomInfo<ZYPKRoom_Cfg> {

    public ZYPKRoom_Set set;
	public long createID;


    
    
    
    
    public static SZYPK_GetRoomInfo make(long roomID, String key, int createSec,PrizeType prizeType,
    		RoomState state, int setID, long ownerID, ZYPKRoom_Cfg cfg, ZYPKRoom_Set set,
		List<RoomPosInfo> posList, Room_Dissolve dissolve,long createID) {
        SZYPK_GetRoomInfo ret = new SZYPK_GetRoomInfo();
		ret.setRoomID(roomID);
		ret.setKey(key);
		ret.setCreateSec(createSec);
		ret.setPrizeType(prizeType);
		ret.setState(state);
		ret.setSetID(setID);
		ret.setOwnerID(ownerID);
		ret.setCfg(cfg);
		ret.set = set;
		ret.setPosList(posList);
		ret.setDissolve(dissolve);
        ret.createID = createID;
        //ret.gameType = gameType;
        //ret.playerResult = playerResult;
        return ret;
    }
}
