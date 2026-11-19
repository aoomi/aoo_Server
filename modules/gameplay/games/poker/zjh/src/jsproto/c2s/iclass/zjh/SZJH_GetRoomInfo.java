package jsproto.c2s.iclass.zjh;
import java.util.List;

import cenum.room.RoomState;
import cenum.PrizeType;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.GetRoomInfo;
import jsproto.c2s.cclass.room.RoomPosInfo;
import jsproto.c2s.cclass.room.Room_Dissolve;
import jsproto.c2s.cclass.zjh.ZJHRoom_Cfg;
import jsproto.c2s.cclass.zjh.ZJHRoom_Set;

/*
 * 用户信息
 * @author zaf
 * */
public class SZJH_GetRoomInfo extends GetRoomInfo<ZJHRoom_Cfg> {


    public ZJHRoom_Set set;
	public long createID;
	//public List<ArrayList<PlayerResult>> playerResult;

    
    

    public static SZJH_GetRoomInfo make(long roomID, String key, int createSec,PrizeType prizeType,
		RoomState state, int setID, long ownerID, ZJHRoom_Cfg cfg, ZJHRoom_Set set, List<RoomPosInfo> posList,
    		Room_Dissolve dissolve,long createID/*,List<ArrayList<PlayerResult>> playerResult*/) {
        SZJH_GetRoomInfo ret = new SZJH_GetRoomInfo();
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
