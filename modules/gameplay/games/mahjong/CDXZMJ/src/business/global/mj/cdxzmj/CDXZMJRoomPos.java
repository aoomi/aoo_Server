package business.global.mj.cdxzmj;		
		
import business.global.room.base.AbsBaseRoom;		
import business.global.room.mj.MJRoomPos;		
		
/**		
 * 房间内每个位置信息		
 *		
 * @param <T>		
 * @author Huaxing		
 */		
		
public class CDXZMJRoomPos<T> extends MJRoomPos {		
    private int dissolveCount = 0;		
    /**		
     * 小局结算托管次数		
     */		
    private int setEndTrusteeshipCount = 0;		
    public CDXZMJRoomPos(int posID, AbsBaseRoom room) {		
        super(posID, room);		
    }		
		
		
    @Override		
    public void setTrusteeship(boolean isTrusteeship) {		
        if (!isTrusteeship) {		
            //小局结算托管次数		
            clearSetEndTrusteeshipCount();		
        }		
        super.setTrusteeship(isTrusteeship);		
    }		
		
    public int getSetEndTrusteeshipCount() {		
        return this.isTrusteeship() ? setEndTrusteeshipCount : 0;		
    }		
		
    public void addSetEndTrusteeshipCount() {		
        if (this.isTrusteeship()) {		
            this.setEndTrusteeshipCount++;		
        }		
    }		
		
    public void clearSetEndTrusteeshipCount() {		
        this.setEndTrusteeshipCount = 0;		
    }		
		
		
    public int getDissolveCount() {		
        return dissolveCount;		
    }		
		
    public void addDissolveCount() {		
        this.dissolveCount++;		
    }		
		
}		
