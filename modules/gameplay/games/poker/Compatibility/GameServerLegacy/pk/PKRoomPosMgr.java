package business.global.pk;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;

public class PKRoomPosMgr extends AbsRoomPosMgr {

    public PKRoomPosMgr(AbsBaseRoom room) {
        super(room);
    }

    @Override
    protected void initPosList() {
        // 初始化房间位置
        for (int posID = 0; posID < this.getPlayerNum(); posID++) {
            this.posList.add(new PKRoomPos(posID, room));
        }
    }

    @Override
    public PKRoomPos addWatch() {
        PKRoomPos newWatch = new PKRoomPos(-1, this.getRoom());
        this.watchList.add(newWatch);
        return newWatch;
    }

    @Override
    protected AbsRoomPos addAll() {
        PKRoomPos allWatch = new PKRoomPos(-1, this.getRoom());
        this.allList.add(allWatch);
        return allWatch;
    }

}
