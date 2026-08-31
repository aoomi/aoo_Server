package business.global.mj.cdxzmj;		
		
import business.global.room.base.AbsBaseRoom;		
import business.global.room.mj.MJRoomPosMgr;		
		
public class CDXZMJRoomMgr extends MJRoomPosMgr {		
    public CDXZMJRoomMgr(AbsBaseRoom room) {		
        super(room);		
    }		
		
    @Override		
    protected void initPosList() {		
        // 初始化房间位置		
        for (int posID = 0; posID < this.getPlayerNum(); posID++) {		
            this.posList.add(new CDXZMJRoomPos<>(posID, room));		
        }		
    }		
    /**		
     * 检查小局结算托管次数		
     *		
     * @return		
     */		
    public boolean checkSetEndTrusteeship() {		
        this.posList.stream().forEach(k -> ((CDXZMJRoomPos) k).addSetEndTrusteeshipCount());		
        return this.posList.stream().anyMatch(k -> ((CDXZMJRoomPos) k).getSetEndTrusteeshipCount() >= 2);		
    }		
}		
