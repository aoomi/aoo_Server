package business.global.room.mj;

import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomPosMgr;

public class MJRoomPosMgr extends AbsRoomPosMgr {

    public MJRoomPosMgr(AbsBaseRoom room) {
        super(room);
    }

    @Override
    protected void initPosList() {
        // 初始化房间位置
        for (int posID = 0; posID < this.getPlayerNum(); posID++) {
            this.posList.add(new MJRoomPos(posID, room));
        }
    }

    @Override
    public MJRoomPos addWatch() {
        MJRoomPos newWatch = new MJRoomPos(-1, this.getRoom());
        this.watchList.add(newWatch);
        return newWatch;
    }

    @Override
    protected AbsRoomPos addAll() {
        MJRoomPos allWatch = new MJRoomPos(-1, this.getRoom());
        this.allList.add(allWatch);
        return allWatch;
    }

}
