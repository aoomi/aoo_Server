package business.global.mj.scjymj;

import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJPiao;
import business.global.room.base.AbsBaseRoom;
import business.global.room.mj.MJRoomPos;

/**
 * 房间内每个位置信息
 *
 * @author Huaxing
 */
public class SCJYMJRoomPos extends MJRoomPos {
    private SCJYMJPiao aPiao = SCJYMJPiao.Error;

    public SCJYMJRoomPos(int posID, AbsBaseRoom room) {
        super(posID, room);
    }

    public SCJYMJPiao getaPiao() {
        return aPiao;
    }

    public void setaPiao(SCJYMJPiao aPiao) {
        if (!SCJYMJPiao.Error.equals(this.aPiao) || SCJYMJPiao.Error.equals(aPiao)) {
            return;
        }
        this.aPiao = aPiao;
    }

}
