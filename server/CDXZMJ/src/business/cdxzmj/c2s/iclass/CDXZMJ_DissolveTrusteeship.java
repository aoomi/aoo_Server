package business.cdxzmj.c2s.iclass;		
		
import jsproto.c2s.cclass.BaseSendMsg;		
		
import java.util.List;		
		
public class CDXZMJ_DissolveTrusteeship extends BaseSendMsg {				
    // 房间ID									
    private long roomID;									
    // 托管玩家									
    private List<Long> trusteeshipPlayerList;									
    // 结束时间									
    private int endSec;									
									
    public static CDXZMJ_DissolveTrusteeship make(long roomID, List<Long> trusteeshipPlayerList, int endSec) {				
        CDXZMJ_DissolveTrusteeship ret = new CDXZMJ_DissolveTrusteeship();				
        ret.setRoomID(roomID);									
        ret.setTrusteeship(trusteeshipPlayerList);									
        ret.setEndSec(endSec);									
        return ret;									
    }									
									
    public long getRoomID() {									
        return roomID;									
    }									
									
    public void setRoomID(long roomID) {									
        this.roomID = roomID;									
    }									
									
    public void setTrusteeship(List<Long> trusteeshipPlayerList) {									
        this.trusteeshipPlayerList = trusteeshipPlayerList;									
    }									
									
    public int getEndSec() {									
        return endSec;									
    }									
									
    public void setEndSec(int endSec) {									
        this.endSec = endSec;									
    }									
									
}									
